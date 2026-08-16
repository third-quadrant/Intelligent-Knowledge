package com.third_quadrant.intelligentknowledge.knowledge.petrology;

import com.google.common.collect.Lists;
// 암석 분석기 블록/메뉴 타입은 registry 패키지에서 등록한 것을 사용한다.
import com.third_quadrant.intelligentknowledge.registry.ModBlocks;
import com.third_quadrant.intelligentknowledge.registry.ModMenus;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// 암석 분석기 메뉴. 바닐라 석재절단기 메뉴와 동일한 구조이며,
// 레시피 대신 "암석 목록(RockTypes)"에서 입력과 다른 암석으로 1:1 변환만 수행한다.
public class RockAnalyzerMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    private static final int INV_SLOT_START = 2;
    private static final int INV_SLOT_END = 29;
    private static final int USE_ROW_SLOT_START = 29;
    private static final int USE_ROW_SLOT_END = 38;

    private final ContainerLevelAccess access;
    // 현재 선택된 변환 대상의 인덱스. (클라이언트-서버 동기화용 DataSlot)
    private final DataSlot selectedRecipeIndex = DataSlot.standalone();
    private final Level level;
    // 현재 입력 슬롯의 암석으로 변환할 수 있는 대상 목록.
    private List<ItemStack> recipes = Lists.newArrayList();
    // 직전에 처리한 입력 스택. (변경 감지용)
    private ItemStack input = ItemStack.EMPTY;
    // 마지막으로 결과를 꺼낸 게임 틱. 같은 틱에 소리가 반복 재생되는 것을 방지한다.
    long lastSoundTime;
    final Slot inputSlot;
    final Slot resultSlot;
    Runnable slotUpdateListener = () -> {
    };
    // 입력 1칸짜리 컨테이너. 내용이 바뀌면 slotsChanged()로 변환 목록을 다시 계산한다.
    public final Container container = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            RockAnalyzerMenu.this.slotsChanged(this);
            RockAnalyzerMenu.this.slotUpdateListener.run();
        }
    };
    // 결과가 잠시 보관되는 컨테이너. (실제 인벤토리는 아님)
    final ResultContainer resultContainer = new ResultContainer();

    // 클라이언트에서 메뉴를 열 때 쓰는 생성자. (접근 불가, 서버와 같은 화면을 그리기만 함)
    public RockAnalyzerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    // 서버에서 실제 메뉴를 만들 때 쓰는 생성자.
    public RockAnalyzerMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenus.ROCK_ANALYZER.get(), containerId);
        this.access = access;
        this.level = playerInventory.player.level();
        // 입력 슬롯(좌측)과 결과 슬롯(우측). 위치는 석재절단기 GUI와 동일하다.
        this.inputSlot = this.addSlot(new Slot(this.container, 0, 20, 33));
        this.resultSlot = this.addSlot(new Slot(this.resultContainer, 1, 143, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            // 결과를 꺼내면 입력 1개를 소모하고 결과를 다시 계산한다.
            @Override
            public void onTake(Player player, ItemStack stack) {
                stack.onCraftedBy(player.level(), player, stack.getCount());
                ItemStack itemstack = RockAnalyzerMenu.this.inputSlot.remove(1);
                if (!itemstack.isEmpty()) {
                    RockAnalyzerMenu.this.setupResultSlot();
                }

                access.execute((pLevel, pPos) -> {
                    long l = pLevel.getGameTime();
                    if (RockAnalyzerMenu.this.lastSoundTime != l) {
                        pLevel.playSound(null, pPos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                        RockAnalyzerMenu.this.lastSoundTime = l;
                    }
                });
                super.onTake(player, stack);
            }
        });

        // 플레이어 인벤토리(3줄) + 핫바(1줄) 슬롯.
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int k = 0; k < 9; k++) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }

        this.addDataSlot(this.selectedRecipeIndex);
    }

    public int getSelectedRecipeIndex() {
        return this.selectedRecipeIndex.get();
    }

    public List<ItemStack> getRecipes() {
        return this.recipes;
    }

    public int getNumRecipes() {
        return this.recipes.size();
    }

    // 입력 슬롯에 변환 가능한 암석이 들어 있어야 화면에 목록을 표시한다.
    public boolean hasInputItem() {
        return this.inputSlot.hasItem() && !this.recipes.isEmpty();
    }

    // 메뉴가 열린 상태에서 플레이어가 블록과의 거리를 벗어나면 닫힌다.
    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.ROCK_ANALYZER.get());
    }

    // GUI의 변환 목록을 클릭했을 때 발생. 선택 인덱스만 바꾸고 결과 슬롯을 갱신한다.
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.isValidRecipeIndex(id)) {
            this.selectedRecipeIndex.set(id);
            this.setupResultSlot();
        }

        return true;
    }

    private boolean isValidRecipeIndex(int recipeIndex) {
        return recipeIndex >= 0 && recipeIndex < this.recipes.size();
    }

    // 입력 슬롯 내용이 바뀌었을 때 호출된다.
    @Override
    public void slotsChanged(Container inventory) {
        ItemStack itemstack = this.inputSlot.getItem();
        if (!itemstack.is(this.input.getItem())) {
            this.input = itemstack.copy();
            this.setupRecipeList(inventory, itemstack);
        }
    }

    // 입력 암석을 바탕으로 변환 가능한 대상 목록을 다시 만든다.
    private void setupRecipeList(Container container, ItemStack stack) {
        this.recipes.clear();
        this.selectedRecipeIndex.set(-1);
        this.resultSlot.set(ItemStack.EMPTY);
        if (!stack.isEmpty() && RockTypes.isRock(stack)) {
            this.recipes = RockTypes.conversionTargets(stack);
        }
    }

    // 선택된 변환 대상(index)을 결과 슬롯에 올린다.
    void setupResultSlot() {
        if (!this.recipes.isEmpty() && this.isValidRecipeIndex(this.selectedRecipeIndex.get())) {
            ItemStack itemstack = this.recipes.get(this.selectedRecipeIndex.get()).copy();
            this.resultSlot.set(itemstack);
        } else {
            this.resultSlot.set(ItemStack.EMPTY);
        }

        this.broadcastChanges();
    }

    @Override
    public MenuType<?> getType() {
        return ModMenus.ROCK_ANALYZER.get();
    }

    // 클라이언트 화면이 "내용이 바뀌었을 때" 콜백을 등록하는 용도. (석재절단기와 동일)
    public void registerUpdateListener(Runnable listener) {
        this.slotUpdateListener = listener;
    }

    // 더블클릭으로 전체 스택을 옮길 때 결과 슬롯은 허용하지 않는다.
    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultContainer && super.canTakeItemForPickAll(stack, slot);
    }

    // Shift 클릭 이동 처리. 암석이면 입력 슬롯으로, 아니면 인벤토리/핫바 사이로 이동한다.
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            Item item = itemstack1.getItem();
            itemstack = itemstack1.copy();
            if (index == 1) {
                item.onCraftedBy(itemstack1, player.level(), player);
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (index == 0) {
                if (!this.moveItemStackTo(itemstack1, 2, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (RockTypes.isRock(itemstack1)) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 2 && index < 29) {
                if (!this.moveItemStackTo(itemstack1, 29, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 29 && index < 38 && !this.moveItemStackTo(itemstack1, 2, 29, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            }

            slot.setChanged();
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
            this.broadcastChanges();
        }

        return itemstack;
    }

    // 메뉴가 닫힐 때 입력 컨테이너의 남은 아이템을 플레이어에게 돌려준다.
    @Override
    public void removed(Player player) {
        super.removed(player);
        this.resultContainer.removeItemNoUpdate(1);
        this.access.execute((pLevel, pPos) -> this.clearContainer(player, this.container));
    }
}
