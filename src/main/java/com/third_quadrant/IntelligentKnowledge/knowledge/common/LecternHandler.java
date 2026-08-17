package com.third_quadrant.intelligentknowledge.knowledge.common;

import com.third_quadrant.intelligentknowledge.attachment.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// 독서대 인터랙션 처리. 이벤트 방식으로 Mixin 없이 처리한다.
// - 배치: #minecraft:lectern_books 태그로 자동 처리.
// - 우클릭: 지식책 UI 열기 (공부는 UI에서 시작).
// - 쉬프트+우클릭: 책 회수.
public class LecternHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // 독서대가 아니면 무시.
        if (!state.is(Blocks.LECTERN)) return;
        // 독서대에 책이 없으면 무시.
        if (!state.getValue(LecternBlock.HAS_BOOK)) return;
        // 블록 엔티티가 없으면 무시.
        if (!(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern)) return;
        ItemStack bookStack = lectern.getBook();

        // 랜덤 지식책인지 검증.
        if (!(bookStack.getItem() instanceof RandomKnowledgeBookItem rkb)) return;

        // 바닐라 동작 차단.
        event.setCanceled(true);

        // 서버에서만 실제 로직을 처리한다.
        if (!level.isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();

            if (event.getEntity().isSecondaryUseActive()) {
                // === 쉬프트+우클릭: 책 회수 ===
                handleRetrieve(serverPlayer, level, pos, state, lectern, bookStack);
            } else if (RandomKnowledgeBookItem.isIdentified(bookStack)) {
                // === 이미 확인된 책 — 바로 UI 열기 ===
                KnowledgeBookDefinition def = RandomKnowledgeBookItem.getDefinition(bookStack);
                if (def != null) {
                    handleOpenUI(serverPlayer, def, pos);
                }
            } else {
                // === 미확인 랜덤 책 — 즉시 확인 후 UI 열기 ===
                String tierLabel = rkb.getTierLabel();
                KnowledgeBookDefinition identified = RandomKnowledgeBookItem.identifyBook(bookStack, tierLabel);
                if (identified != null) {
                    // lectern에 확인된 책 상태를 저장 (setChanged 포함).
                    lectern.setBook(bookStack);

                    String tierColor = switch (identified.getTier()) {
                        case MASTER -> "§b";
                        case PHD -> "§d";
                        default -> "§a";
                    };
                    String rarityColor = "§" + identified.bookRarity().getChatFormatting().getChar();
                    event.getEntity().displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§a책을 훑어보았습니다! " + tierColor + "[" + identified.getTier().getDisplayName() + "] "
                                            + rarityColor + "[" + identified.bookRarity().getDisplayNameKo() + "] " + identified.title()),
                            false);
                    level.playSound(null, event.getEntity().blockPosition(),
                            net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                            SoundSource.PLAYERS, 1.0F, 1.0F);
                    handleOpenUI(serverPlayer, identified, pos);
                } else {
                    event.getEntity().displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§c책 확인 실패."), false);
                }
            }
        }
    }

    // 지식책 UI 열기 (서버). 읽기는 UI에서 공부 버튼으로 시작.
    private static void handleOpenUI(ServerPlayer player, KnowledgeBookDefinition def, BlockPos pos) {
        // 현재 책 진행도 조회.
        var progressMap = player.getData(ModAttachments.BOOK_READ_PROGRESS);
        int currentBookShards = progressMap.getOrDefault(def.id(), 0);

        // UI 열기 (독서대 위치 전달).
        KnowledgeBookMenu.open(player, def, currentBookShards, pos);
    }

    // 지식 책 회수 처리 (서버).
    private static void handleRetrieve(ServerPlayer player, Level level, BlockPos pos,
                                       BlockState state, LecternBlockEntity lectern, ItemStack bookStack) {
        // 인벤토리에 책 추가.
        ItemStack returnStack = bookStack.copy();
        if (!player.getInventory().add(returnStack)) {
            player.drop(returnStack, false);
        }

        // 독서대 비우기 + 블록 상태 초기화.
        lectern.clearContent();
        LecternBlock.resetBookState(null, level, pos, state, false);

        // 회수 효과음.
        level.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
