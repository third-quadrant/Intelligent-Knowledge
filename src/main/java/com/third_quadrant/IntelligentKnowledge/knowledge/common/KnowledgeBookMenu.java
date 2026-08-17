package com.third_quadrant.intelligentknowledge.knowledge.common;

import com.third_quadrant.intelligentknowledge.attachment.ModAttachments;
import com.third_quadrant.intelligentknowledge.item.NoteItem;
import com.third_quadrant.intelligentknowledge.registry.ModItems;
import com.third_quadrant.intelligentknowledge.registry.ModMenus;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

// 지식 책 전용 메뉴. 인벤토리 슬롯 없이 DataSlots로 서버-클라이언트 간 데이터를 동기화한다.
// 공부(독서) 타이머와 가져가기 버튼을 서버에서 관리한다.
public class KnowledgeBookMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger("IntelligentKnowledge");

    // 버튼 ID.
    public static final int BUTTON_TAKE = 0;
    public static final int BUTTON_STUDY = 1;

    private final String bookId;

    // 서버 전용: 독서대 위치.
    private final BlockPos lecternPos;

    // DataSlot 참조 (서버-클라이언트 동기화 데이터).
    private final DataSlot dsDifficulty;
    private final DataSlot dsKnowledge;
    private final DataSlot dsProgress;
    private final DataSlot dsMaxShards;
    private final DataSlot dsBaseReward;
    private final DataSlot dsStudyState;
    private final DataSlot dsStudyPercent;
    private final DataSlot dsGrantedShards;

    // ─── 서버 측 공부 관리 (정적) ───
    private static final Map<UUID, ActiveStudy> ACTIVE_STUDIES = new HashMap<>();

    private static class ActiveStudy {
        final UUID playerUuid;
        final KnowledgeBookMenu menu;
        final String bookId;
        int durationTicks;
        final BlockPos lecternPos;
        int totalReward;
        int cycleTicks;
        int elapsedTicks;
        int grantedShards;
        int cycleCount;
        long lastSoundTick;

        ActiveStudy(UUID uuid, KnowledgeBookMenu menu, String bookId, int duration,
                    BlockPos pos, int totalReward) {
            this.playerUuid = uuid;
            this.menu = menu;
            this.bookId = bookId;
            this.durationTicks = duration;
            this.lecternPos = pos;
            this.totalReward = totalReward;
            this.cycleTicks = totalReward > 0 ? Math.max(1, duration / totalReward) : duration;
            this.elapsedTicks = 0;
            this.grantedShards = 0;
            this.cycleCount = 0;
            this.lastSoundTick = 0;
        }
    }

    // 서버에서 실제 메뉴를 열 때.
    public KnowledgeBookMenu(int containerId, Inventory playerInventory, String bookId,
                             int bookDifficulty, int playerKnowledge, int bookProgress,
                             int maxShards, int baseReward, BlockPos lecternPos) {
        super(ModMenus.KNOWLEDGE_BOOK.get(), containerId);
        this.bookId = bookId;
        this.lecternPos = lecternPos;

        // DataSlots: 서버 데이터를 클라이언트에 동기화.
        // 인벤토리 슬롯 없이 DataSlots만 등록. 참조를 저장하여 getter에서 읽을 수 있도록 함.
        this.dsDifficulty = registerDataSlot(bookDifficulty);
        this.dsKnowledge = registerDataSlot(playerKnowledge);
        this.dsProgress = registerDataSlot(bookProgress);
        this.dsMaxShards = registerDataSlot(maxShards);
        this.dsBaseReward = registerDataSlot(baseReward);

        this.dsStudyState = DataSlot.standalone();
        this.dsStudyPercent = DataSlot.standalone();
        this.dsGrantedShards = DataSlot.standalone();
        addDataSlot(dsStudyState);
        addDataSlot(dsStudyPercent);
        addDataSlot(dsGrantedShards);
        dsStudyState.set(0);
        dsStudyPercent.set(0);
        dsGrantedShards.set(0);

        // 인벤토리 슬록 추가하지 않음 — UI에서 슬롯/아이템 렌더링을 방지.
    }

    private DataSlot registerDataSlot(int value) {
        DataSlot ds = DataSlot.standalone();
        addDataSlot(ds);
        ds.set(value);
        return ds;
    }

    // 클라이언트에서 메뉴를 열 때 (동기화된 DataSlot 값을 사용).
    public KnowledgeBookMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, "", 0, 0, 0, 0, 0, null);
    }

    public String getBookId() { return bookId; }
    public int getBookDifficulty() { return dsDifficulty.get(); }
    public int getPlayerKnowledge() { return dsKnowledge.get(); }
    public int getBookProgress() { return dsProgress.get(); }
    public int getMaxShards() { return dsMaxShards.get(); }
    public int getBaseReward() { return dsBaseReward.get(); }
    public BlockPos getLecternPos() { return lecternPos; }
    public boolean isStudying() { return dsStudyState.get() == 1; }
    public int getStudyPercentFromSlot() { return dsStudyPercent.get(); }
    public int getGrantedShards() { return dsGrantedShards.get(); }

    // 스냅샷을 고려한 유효 gap 계산 (클라이언트 렌더링용).
    public int getEffectiveGap(net.minecraft.world.entity.player.Player player, int bookDifficulty) {
        var snapshotMap = player.getData(ModAttachments.BOOK_SNAPSHOT_KNOWLEDGE);
        int snapshotKnowledge = snapshotMap.getOrDefault(bookId, getPlayerKnowledge());
        if (snapshotKnowledge < bookDifficulty) {
            return bookDifficulty - snapshotKnowledge;
        }
        return bookDifficulty - getPlayerKnowledge();
    }

    // ─── 버튼 클릭 처리 (서버에서만 호출) ───
    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (player.level().isClientSide()) return false;

        return switch (buttonId) {
            case BUTTON_TAKE -> handleTake((ServerPlayer) player);
            case BUTTON_STUDY -> handleStudyStart((ServerPlayer) player);
            default -> false;
        };
    }

    // 가져가기 처리.
    private boolean handleTake(ServerPlayer player) {
        if (lecternPos == null) return false;

        // 공부 중이면 즉시 취소.
        cancelStudy(player.getUUID());

        ServerLevel level = player.serverLevel();
        if (!level.getBlockState(lecternPos).is(Blocks.LECTERN)) return false;
        if (!(level.getBlockEntity(lecternPos) instanceof LecternBlockEntity lectern)) return false;

        ItemStack bookStack = lectern.getBook();
        if (bookStack.isEmpty()) return false;
        if (!(bookStack.getItem() instanceof RandomKnowledgeBookItem)) return false;

        // 인벤토리에 책 추가.
        ItemStack returnStack = bookStack.copy();
        if (!player.getInventory().add(returnStack)) {
            player.drop(returnStack, false);
        }

        // 독서대 비우기.
        lectern.clearContent();
        LecternBlock.resetBookState(null, level, lecternPos,
                level.getBlockState(lecternPos), false);
        level.playSound(null, lecternPos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);

        // 메뉴 닫기.
        player.closeContainer();
        return true;
    }

    // 공부 시작 처리.
    private boolean handleStudyStart(ServerPlayer player) {
        if (lecternPos == null) return false;
        if (isStudying()) return false;

        // 독서대 검증.
        ServerLevel level = player.serverLevel();
        if (!level.getBlockState(lecternPos).is(Blocks.LECTERN)) return false;
        if (!(level.getBlockEntity(lecternPos) instanceof LecternBlockEntity lectern)) return false;
        ItemStack bookStack = lectern.getBook();
        if (bookStack.isEmpty() || !(bookStack.getItem() instanceof RandomKnowledgeBookItem)) return false;

        // 난이도 검증.
        int currentDifficulty = getBookDifficulty();
        int currentKnowledge = getPlayerKnowledge();
        int gap = currentDifficulty - currentKnowledge;

        // maxShards 검증.
        int currentMaxShards = getMaxShards();
        int currentProgress = getBookProgress();
        if (currentProgress >= currentMaxShards) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§c이 책에서 얻을 수 있는 지식을 모두 습득했습니다."), true);
            return false;
        }

        // 총 보상 미리 계산 (사이클 결정용). UI의 예상 조각과 동일한 값.
        var snapshotMap = player.getData(ModAttachments.BOOK_SNAPSHOT_KNOWLEDGE);
        int snapshotKnowledge = snapshotMap.getOrDefault(bookId, currentKnowledge);
        int effectiveGap;
        if (snapshotKnowledge < currentDifficulty) {
            effectiveGap = currentDifficulty - snapshotKnowledge;
        } else {
            effectiveGap = gap;
        }
        int totalReward = KnowledgeTier.computeReward(effectiveGap, getBaseReward(),
                currentProgress, currentMaxShards);

        // 공부 시간 계산.
        int duration = KnowledgeTier.computeReadTime(gap);

        // 공부 시작 시점의 지식 조각 스냅샷 저장 (이미 있으면 유지).
        var snapMap = new java.util.HashMap<>(player.getData(ModAttachments.BOOK_SNAPSHOT_KNOWLEDGE));
        snapMap.putIfAbsent(bookId, currentKnowledge);
        player.setData(ModAttachments.BOOK_SNAPSHOT_KNOWLEDGE, snapMap);

        // 공부 상태 저장.
        ActiveStudy study = new ActiveStudy(player.getUUID(), this, bookId, duration, lecternPos, totalReward);
        ACTIVE_STUDIES.put(player.getUUID(), study);
        LOGGER.info("[IK] Study started: book={} duration={}ticks totalReward={} cycleTicks={} lecternPos={}",
                bookId, duration, totalReward, study.cycleTicks, lecternPos);

        // DataSlots 업데이트.
        dsStudyState.set(1);
        dsStudyPercent.set(0);
        dsGrantedShards.set(0);

        return true;
    }

    // 서버에서 공부 완료 처리 — tick에서 이미 대부분 지급됨, 잔여분 처리.
    private void completeStudy(ServerPlayer player, ActiveStudy study) {
        KnowledgeBookDefinition def = KnowledgeRegistry.get(study.bookId);
        if (def == null) {
            player.closeContainer();
            return;
        }

        int granted = study.grantedShards;

        // 완료 효과음.
        ServerLevel level = player.serverLevel();
        if (study.lecternPos != null) {
            level.playSound(null, study.lecternPos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
        }

        // 액션바 메시지.
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§a공부 완료! 지식 조각 +" + granted), true);

        // 마일스톤 체크.
        int finalCount = player.getData(ModAttachments.STONE_KNOWLEDGE_SHARD);
        grantIfMilestone(player, finalCount);

        // 메뉴 닫기.
        player.closeContainer();
    }

    // 서버 틱에서 호출: 모든 활성 공부 상태를 갱신.
    public static void tick(ServerLevel level) {
        HashMap<UUID, ActiveStudy> snapshot = new HashMap<>(ACTIVE_STUDIES);

        for (Map.Entry<UUID, ActiveStudy> entry : snapshot.entrySet()) {
            UUID uuid = entry.getKey();
            ActiveStudy study = entry.getValue();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(study.playerUuid);

            if (player == null || !(player.containerMenu instanceof KnowledgeBookMenu menu)) {
                ACTIVE_STUDIES.remove(uuid);
                continue;
            }
            if (player.serverLevel() != level) continue;

            // 독서대 재검증.
            var blockState = level.getBlockState(study.lecternPos);
            if (!blockState.is(Blocks.LECTERN)) {
                ACTIVE_STUDIES.remove(uuid);
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§c독서대가 파괴되었습니다."), true);
                player.closeContainer();
                continue;
            }

            study.elapsedTicks++;

            // 진행률 업데이트.
            int percent = Math.min(100, (int) ((long) study.elapsedTicks * 100 / study.durationTicks));
            menu.dsStudyState.set(1);
            menu.dsStudyPercent.set(percent);

            // ─── 주기적 보상: cycleTicks마다 1조각 지급 ───
            if (study.totalReward > 0 && study.grantedShards < study.totalReward
                    && study.elapsedTicks >= (long) (study.grantedShards + 1) * study.cycleTicks) {
                int playerShards = player.getData(ModAttachments.STONE_KNOWLEDGE_SHARD);
                player.setData(ModAttachments.STONE_KNOWLEDGE_SHARD, playerShards + 1);

                var pMap = new java.util.HashMap<>(player.getData(ModAttachments.BOOK_READ_PROGRESS));
                int bookProg = pMap.getOrDefault(study.bookId, 0);
                pMap.put(study.bookId, bookProg + 1);
                player.setData(ModAttachments.BOOK_READ_PROGRESS, pMap);
                menu.dsProgress.set(bookProg + 1);

                study.grantedShards++;
                menu.dsGrantedShards.set(study.grantedShards);
            }

            // 효과음.
            if (study.elapsedTicks - study.lastSoundTick >= 40 + level.random.nextInt(40)) {
                level.playSound(null, study.lecternPos, SoundEvents.BOOK_PAGE_TURN,
                        SoundSource.PLAYERS, 0.8F, 1.0F + level.random.nextFloat() * 0.4F);
                study.lastSoundTick = study.elapsedTicks;
            }

            // ─── 사이클 완료 ───
            if (study.elapsedTicks >= study.durationTicks) {
                study.cycleCount++;
                study.elapsedTicks = 0;
                menu.dsStudyPercent.set(0);

                // 전체 보상 소진 시 종료.
                if (study.grantedShards >= study.totalReward) {
                    // 노트 있으면 → 페이지 사용하고 루프 재시작.
                    if (useNote(player)) {
                        KnowledgeBookDefinition def = KnowledgeRegistry.get(study.bookId);
                        if (def != null) {
                            int curKnow = player.getData(ModAttachments.STONE_KNOWLEDGE_SHARD);
                            var snapMap = player.getData(ModAttachments.BOOK_SNAPSHOT_KNOWLEDGE);
                            int snapKnow = snapMap.getOrDefault(study.bookId, curKnow);
                            int effGap = (snapKnow < def.difficulty()) ? def.difficulty() - snapKnow : def.difficulty() - curKnow;
                            // 실제 책 진행도로 남은 보상 계산.
                            var pMap = player.getData(ModAttachments.BOOK_READ_PROGRESS);
                            int actualBookProgress = pMap.getOrDefault(study.bookId, 0);
                            int newTotal = KnowledgeTier.computeReward(effGap, def.baseReward(),
                                    actualBookProgress, def.maxShards());
                            if (newTotal > 0) {
                                int newDur = KnowledgeTier.computeReadTime(def.difficulty() - curKnow);
                                study.totalReward = newTotal;
                                study.durationTicks = Math.max(1, newDur);
                                study.cycleTicks = Math.max(1, study.durationTicks / newTotal);
                                study.grantedShards = 0;
                                study.elapsedTicks = 0;
                                menu.dsStudyPercent.set(0);
                                menu.dsGrantedShards.set(0);
                                LOGGER.info("[IK] Note used, loop restarted: book={} newTotal={} newDur={}",
                                        study.bookId, newTotal, newDur);
                                continue;
                            }
                        }
                    }
                    ACTIVE_STUDIES.remove(uuid);
                    study.menu.completeStudy(player, study);
                    continue;
                }

                // 다음 사이클: gap 재계산으로 duration/cycleTicks 갱신.
                KnowledgeBookDefinition def = KnowledgeRegistry.get(study.bookId);
                if (def == null) {
                    ACTIVE_STUDIES.remove(uuid);
                    study.menu.completeStudy(player, study);
                    continue;
                }
                int curKnow = player.getData(ModAttachments.STONE_KNOWLEDGE_SHARD);
                var snapMap = player.getData(ModAttachments.BOOK_SNAPSHOT_KNOWLEDGE);
                int snapKnow = snapMap.getOrDefault(study.bookId, curKnow);
                int effGap = (snapKnow < def.difficulty()) ? def.difficulty() - snapKnow : def.difficulty() - curKnow;
                int remainReward = study.totalReward - study.grantedShards;
                int newDuration = KnowledgeTier.computeReadTime(def.difficulty() - curKnow);
                study.durationTicks = Math.max(1, newDuration);
                study.cycleTicks = Math.max(1, study.durationTicks / remainReward);
            }
        }
    }

    // 공부 취소 (메뉴가 닫힐 때 호출).
    public static void cancelStudy(UUID playerUuid) {
        ACTIVE_STUDIES.remove(playerUuid);
    }

    // 인벤토리에서 노트1개 분리 → 페이지 사용. 사용 가능하면 true.
    private static boolean useNote(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(ModItems.NOTE.get()) && NoteItem.canUse(stack)) {
                // 스택이2개 이상이면1개 분리.
                if (stack.getCount() > 1) {
                    ItemStack single = stack.split(1);
                    NoteItem.addPage(single);
                    // 분리한 노트를 인벤토리에 추가.
                    if (!inv.add(single)) {
                        player.drop(single, false);
                    }
                } else {
                    NoteItem.addPage(stack);
                    if (!NoteItem.canUse(stack)) {
                        inv.removeItemNoUpdate(i);
                    }
                }
                return true;
            }
        }
        return false;
    }

    // 기존 마일스톤 발전과제 로직.
    private static void grantIfMilestone(ServerPlayer player, int shardCount) {
        int[] milestones = {100, 300, 500, 1000};
        for (int m : milestones) {
            if (shardCount >= m) {
                net.minecraft.advancements.AdvancementHolder holder = player.server.getAdvancements()
                        .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                "intelligentknowledge", "knowledge/stone_" + m));
                if (holder != null) {
                    player.getAdvancements().award(holder, "count");
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (lecternPos == null) return true;
        // 플레이어가 독서대 근처(8블록)에 있는지 확인.
        return player.distanceToSqr(lecternPos.getX() + 0.5, lecternPos.getY() + 0.5,
                lecternPos.getZ() + 0.5) <= 64;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // 메뉴가 닫힐 때 공부 상태 취소.
        if (!player.level().isClientSide()) {
            cancelStudy(player.getUUID());
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    // 서버에서 KnowledgeBookMenu를 열기 위한 정적 헬퍼.
    public static void open(ServerPlayer player, KnowledgeBookDefinition def,
                            int currentBookShards, BlockPos lecternPos) {
        int playerKnowledge = player.getData(ModAttachments.STONE_KNOWLEDGE_SHARD);
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (containerId, playerInventory, p) -> new KnowledgeBookMenu(
                        containerId, playerInventory,
                        def.id(), def.difficulty(), playerKnowledge,
                        currentBookShards, def.maxShards(), def.baseReward(),
                        lecternPos),
                net.minecraft.network.chat.Component.literal(def.title())));
    }
}
