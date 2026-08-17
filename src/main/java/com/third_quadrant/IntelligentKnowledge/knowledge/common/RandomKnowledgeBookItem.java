package com.third_quadrant.intelligentknowledge.knowledge.common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import com.third_quadrant.intelligentknowledge.attachment.ModAttachments;
import com.third_quadrant.intelligentknowledge.registry.ModDataComponents;
import net.minecraft.nbt.CompoundTag;

// 미확인 지식책 아이템. 크레이티브에서 꺼낸 뒤 우클릭하면 랜덤 속성이 확정된다.
// 확정 후에는 KnowledgeBookItem과 동일하게 독서대 배치/공부가 가능하다.
// 미확인 상태에서는 독서대에 올릴 수 없다.
public class RandomKnowledgeBookItem extends KnowledgeBookItem {

    private final int minDifficulty;
    private final int maxDifficulty;
    private final String tierLabel;

    // 랜덤 필드 (현재는 petrology 고정).
    private static final String[] FIELDS = {"petrology"};

    // 랜덤 저자 풀.
    private static final String[] AUTHORS = {
            "화석박사", "암석연구원", "지질탐험가", "고대학자", "광물 수집가",
            "변성학자", "화산학자", "지각연구원", "퇴적학자", "열역학 연구원",
            "지구화학자", "광물학 교수", "화성학자", "암반역학자"
    };

    // ─── 학위 단계 × 희귀도별 종류 이름 풀 ───
    // 학위 단계가 적절한 종류 이름 범위를 결정하고, 그 안에서 희귀도가 품질을 결정한다.
    private static final Map<String, Map<BookRarity, String[]>> TIER_RARITY_NAME_POOL = new HashMap<>();
    static {
        // 입문: 교과서/입문서/기초서 등 초급 학습서적
        Map<BookRarity, String[]> entryPool = new HashMap<>();
        entryPool.put(BookRarity.COMMON, new String[]{
                "교과서", "입문서", "기초서", "참고서", "문제집",
                "요약노트", "워크북", "안내서", "개론서", "핸드북", "학습지"});
        entryPool.put(BookRarity.UNCOMMON, new String[]{
                "개념서", "심화서", "실습서", "강의노트", "응용서"});
        entryPool.put(BookRarity.RARE, new String[]{
                "전공서적", "원리서", "이론서", "실전서", "정석"});
        entryPool.put(BookRarity.EPIC, new String[]{
                "문제해설집", "문제집 해설", "고급 개론"});
        entryPool.put(BookRarity.LEGENDARY, new String[]{
                "세미나자료집", "학습 안내서", "기초 총람"});
        TIER_RARITY_NAME_POOL.put("입문", entryPool);

        // 학사: 교과서/개념서/논문 등 학부 수준
        Map<BookRarity, String[]> bachelorPool = new HashMap<>();
        bachelorPool.put(BookRarity.COMMON, new String[]{
                "교과서", "문제집", "워크북", "실습서", "학습지"});
        bachelorPool.put(BookRarity.UNCOMMON, new String[]{
                "개념서", "심화서", "원리서", "이론서", "실전서", "강의노트"});
        bachelorPool.put(BookRarity.RARE, new String[]{
                "논문", "연구노트", "문제해설집", "정석", "응용서"});
        bachelorPool.put(BookRarity.EPIC, new String[]{
                "학술지", "저널", "리뷰논문", "백서", "세미나자료집"});
        bachelorPool.put(BookRarity.LEGENDARY, new String[]{
                "학위논문", "전문서", "필사본노트", "전공서적"});
        TIER_RARITY_NAME_POOL.put("학사", bachelorPool);

        // 석사: 논문/학술지/연구노트 등 대학원 수준
        Map<BookRarity, String[]> masterPool = new HashMap<>();
        masterPool.put(BookRarity.COMMON, new String[]{
                "논문", "연구노트", "학술지", "강연록"});
        masterPool.put(BookRarity.UNCOMMON, new String[]{
                "리뷰논문", "저널", "필사본노트", "전문서", "백서"});
        masterPool.put(BookRarity.RARE, new String[]{
                "세미나자료집", "학위논문", "강연록"});
        masterPool.put(BookRarity.EPIC, new String[]{
                "미발간원고", "비공개연구노트", "학회발표자료"});
        masterPool.put(BookRarity.LEGENDARY, new String[]{
                "원본논문", "고서", "봉인된연구일지", "전설의필사본"});
        TIER_RARITY_NAME_POOL.put("석사", masterPool);

        // 박사: 고서/원본논문/금서 등 최고 수준
        Map<BookRarity, String[]> phdPool = new HashMap<>();
        phdPool.put(BookRarity.COMMON, new String[]{
                "연구노트", "논문", "학술지", "저널", "강연록"});
        phdPool.put(BookRarity.UNCOMMON, new String[]{
                "리뷰논문", "필사본노트", "전문서", "백서", "세미나자료"});
        phdPool.put(BookRarity.RARE, new String[]{
                "학위논문", "미발간원고", "비공개연구노트", "학회발표자료"});
        phdPool.put(BookRarity.EPIC, new String[]{
                "절판서", "유고집", "개인소장필사본", "기밀연구자료", "학파비전서"});
        phdPool.put(BookRarity.LEGENDARY, new String[]{
                "고서", "원본논문", "봉인된연구일지", "전설의필사본",
                "잃어버린원전", "시조서", "금서", "태초의기록", "비전"});
        TIER_RARITY_NAME_POOL.put("박사", phdPool);
    }

    // 희귀도 가중치: 일반50, 고급30, 희귀15, 매희4, 전설1 (합계 100).
    private static final BookRarity[] RARITY_TABLE;
    static {
        RARITY_TABLE = new BookRarity[100];
        int idx = 0;
        for (int i = 0; i < 50; i++) RARITY_TABLE[idx++] = BookRarity.COMMON;
        for (int i = 0; i < 30; i++) RARITY_TABLE[idx++] = BookRarity.UNCOMMON;
        for (int i = 0; i < 15; i++) RARITY_TABLE[idx++] = BookRarity.RARE;
        for (int i = 0; i < 4; i++) RARITY_TABLE[idx++] = BookRarity.EPIC;
        RARITY_TABLE[99] = BookRarity.LEGENDARY;
    }

    // ─── 희귀도별 maxShards: 학위 단계 상한선 × 희귀도 배율 ───
    // 학위 단계(입문/학사/석사/박사)가 상한선을 결정하고,
    // 그 안에서 희귀도(일반~전설)가 실제로 얻을 수 있는 양을 결정한다.

    // 학위 단계별 상한선 (널널하게 설정).
    private static int getTierCap(String tierLabel) {
        return switch (tierLabel) {
            case "입문" -> 200;
            case "학사" -> 500;
            case "석사" -> 1000;
            case "박사" -> 2000;
            default -> 100;
        };
    }

    // 희귀도별 배율 (상한선 대비).
    private static double getRarityMultiplier(BookRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0.25;
            case UNCOMMON -> 0.50;
            case RARE -> 0.75;
            case EPIC -> 0.90;
            case LEGENDARY -> 1.00;
        };
    }

    private static int getMaxShardsForRarity(String tierLabel, BookRarity rarity) {
        int cap = getTierCap(tierLabel);
        double mult = getRarityMultiplier(rarity);
        return Math.max(1, (int) Math.round(cap * mult));
    }

    // 희귀도별 기본 보상 (baseReward).
    private static int getBaseRewardForRarity(BookRarity rarity) {
        return switch (rarity) {
            case COMMON -> 6;
            case UNCOMMON -> 15;
            case RARE -> 25;
            case EPIC -> 40;
            case LEGENDARY -> 60;
        };
    }

    public RandomKnowledgeBookItem(Properties properties, int minDifficulty, int maxDifficulty, String tierLabel) {
        super("random_unidentified", properties);
        this.minDifficulty = minDifficulty;
        this.maxDifficulty = maxDifficulty;
        this.tierLabel = tierLabel;
    }

    public String getTierLabel() { return tierLabel; }

    // ─── 확인 여부 ───
    public static boolean isIdentified(ItemStack stack) {
        return stack.has(ModDataComponents.RANDOM_BOOK_DATA.get());
    }

    public static String getBookId(ItemStack stack) {
        CompoundTag tag = stack.get(ModDataComponents.RANDOM_BOOK_DATA.get());
        return tag != null ? tag.getString("bookId") : null;
    }

    public static KnowledgeBookDefinition getDefinition(ItemStack stack) {
        String id = getBookId(stack);
        return id != null ? KnowledgeRegistry.get(id) : null;
    }

    // ─── 미확인 상태에서 블록 설치 차단 (독서대 올리기 방지) ───
    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!isIdentified(stack)) {
            // 미확인 상태 — 블록 설치(독서대 포함) 불가.
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.literal("§7먼저 책을 훑어보세요."), false);
            }
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }

    // ─── 우클릭: 랜덤 속성 확정 (鹔어보기) ───
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (isIdentified(stack)) {
            return super.use(level, player, hand);
        }

        if (!level.isClientSide()) {
            ThreadLocalRandom rand = ThreadLocalRandom.current();
            KnowledgeBookDefinition def = generateRandomBook(rand);
            if (def == null) {
                player.displayClientMessage(Component.literal("§c랜덤 책 생성 실패."), false);
                return InteractionResultHolder.fail(stack);
            }

            KnowledgeRegistry.registerDynamic(def);

            // ItemStack에 확인 데이터 저장.
            CompoundTag tag = new CompoundTag();
            tag.putString("bookId", def.id());
            stack.set(ModDataComponents.RANDOM_BOOK_DATA.get(), tag);

            // 아이템 이름을 실제 책 제목으로 변경 (희귀도 색상 적용).
            stack.set(DataComponents.CUSTOM_NAME,
                    Component.literal(def.title()).withStyle(def.bookRarity().getChatFormatting()));

            // 확인 메시지.
            String tierColor = switch (def.getTier()) {
                case MASTER -> "§b";
                case PHD -> "§d";
                default -> "§a";
            };
            String rarityColor = "§" + def.bookRarity().getChatFormatting().getChar();
            player.displayClientMessage(
                    Component.literal("§a책을 훑어보았습니다! " + tierColor + "[" + def.getTier().getDisplayName() + "] "
                            + rarityColor + "[" + def.bookRarity().getDisplayNameKo() + "] " + def.title()),
                    false);

            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        KnowledgeBookDefinition def = getDefinition(stack);
        if (def != null) return def.difficulty() >= 500;
        return false;
    }

    // ─── 툴팁 ───
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        if (!isIdentified(stack)) {
            tooltip.add(Component.literal("§8미확인 지식책 (" + tierLabel + ")"));
            tooltip.add(Component.literal("§7우클릭하여 훑어보기"));
            return;
        }

        KnowledgeBookDefinition def = getDefinition(stack);
        if (def == null) return;

        String typeName = def.bookType().getDisplayNameKo();
        String rarityName = def.bookRarity().getDisplayNameKo();
        int rarityColor = def.bookRarity().getColor();
        tooltip.add(Component.literal("")
                .append(Component.literal("[" + typeName + "]").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAA9966))))
                .append(Component.literal(" "))
                .append(Component.literal("[" + rarityName + "]").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rarityColor)))));

        tooltip.add(Component.literal("")
                .append(Component.literal("[저자] ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x808080))))
                .append(Component.literal(def.author()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)))));

        tooltip.add(Component.literal("")
                .append(Component.literal("[학문] ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x808080))))
                .append(Component.literal(def.field()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)))));

        String tierName = def.getTier().getDisplayName();
        tooltip.add(Component.literal("")
                .append(Component.literal("[지식 수준] ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x808080))))
                .append(Component.literal(tierName + " (" + def.difficulty() + ")").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)))));

        tooltip.add(Component.literal(""));

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        int currentBookShards = player.getData(ModAttachments.BOOK_READ_PROGRESS)
                .getOrDefault(def.id(), 0);

        tooltip.add(Component.literal("[이해도] ")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x808080)))
                .append(Component.literal(currentBookShards + " / " + def.maxShards())
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)))));

        double progress = def.maxShards() > 0 ? (double) currentBookShards / def.maxShards() : 0;
        int filled = (int) Math.round(progress * 10);
        int empty = 10 - filled;
        String bar = "§a" + "█".repeat(Math.max(0, filled))
                + "§8" + "░".repeat(Math.max(0, empty));
        int percent = (int) Math.round(progress * 100);
        tooltip.add(Component.literal(bar + " " + percent + "%"));

        int playerKnowledge = player.getData(ModAttachments.STONE_KNOWLEDGE_SHARD);
        int gap = def.difficulty() - playerKnowledge;
        if (gap <= 0) {
            int absGap = -gap;
            String rewardText;
            if (absGap >= 100) rewardText = "보상 없음";
            else if (absGap >= 50) rewardText = "보상 1개";
            else rewardText = "보상 2개";
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§7수준 이하 — " + rewardText));
        }
    }

    // ─── 독서대 위 미확인 책 자동 확인 (LecternHandler에서 호출) ───
    // ItemStack의 DataComponent를 설정하고 동적 레지스트리에 등록.
    // 성공하면 KnowledgeBookDefinition을 반환, 실패하면 null.
    public static KnowledgeBookDefinition identifyBook(ItemStack stack, String tierLabel) {
        if (isIdentified(stack)) return getDefinition(stack);
        if (!(stack.getItem() instanceof RandomKnowledgeBookItem rkb)) return null;

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        KnowledgeBookDefinition def = rkb.generateRandomBook(rand);
        if (def == null) return null;

        KnowledgeRegistry.registerDynamic(def);

        CompoundTag tag = new CompoundTag();
        tag.putString("bookId", def.id());
        stack.set(ModDataComponents.RANDOM_BOOK_DATA.get(), tag);
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(def.title()).withStyle(def.bookRarity().getChatFormatting()));

        return def;
    }

    // ─── 랜덤 KnowledgeBookDefinition 생성 ───
    private KnowledgeBookDefinition generateRandomBook(ThreadLocalRandom rand) {
        String bookId = "random_" + System.currentTimeMillis() + "_" + rand.nextInt(10000);

        // 난이도.
        int difficulty = minDifficulty + rand.nextInt(maxDifficulty - minDifficulty + 1);

        // 필드.
        String field = FIELDS[rand.nextInt(FIELDS.length)];

        // 희귀도: 가중 랜덤.
        BookRarity bookRarity = RARITY_TABLE[rand.nextInt(100)];

        // 종류 이름: 학위 단계 × 희귀도별 풀에서 선택.
        Map<BookRarity, String[]> tierPool = TIER_RARITY_NAME_POOL.getOrDefault(tierLabel, TIER_RARITY_NAME_POOL.get("입문"));
        String[] namePool = tierPool.getOrDefault(bookRarity, new String[]{"교과서"});
        String typeName = namePool[rand.nextInt(namePool.length)];

        // BookType 매핑 (투표용).
        BookType bookType = mapNameToType(typeName);

        // 저자.
        String author = AUTHORS[rand.nextInt(AUTHORS.length)];

        // 제목: "{field}의 {종류이름}".
        String title = "암석학의 " + typeName;

        // 희귀도 기반 maxShards (학위 단계 상한선 × 희귀도 배율), baseReward.
        int maxShards = getMaxShardsForRarity(tierLabel, bookRarity);
        int baseReward = getBaseRewardForRarity(bookRarity);

        return new KnowledgeBookDefinition(bookId, title, author, difficulty, field,
                maxShards, baseReward, bookType, bookRarity);
    }

    // 이름 문자열 → BookType 매핑.
    private static BookType mapNameToType(String name) {
        return switch (name) {
            case "교과서", "기초서", "개론서", "핸드북", "고급 개론" -> BookType.TEXTBOOK;
            case "입문서", "안내서", "학습지", "학습 안내서" -> BookType.INTRODUCTION;
            case "전문서", "전공서적", "원리서", "이론서", "심화서" -> BookType.SPECIALIZED;
            case "참고서", "백서", "기초 총람", "세미나자료집", "세미나자료" -> BookType.ENCYCLOPEDIA;
            case "논문", "학술지", "연구노트", "학위논문", "리뷰논문", "저널" -> BookType.RESEARCH_PAPER;
            case "실습서", "실전서" -> BookType.EXPERIMENTAL_MANUAL;
            case "강연록" -> BookType.FIELD_REPORT;
            case "고서", "시조서", "금서", "태초의기록", "비전",
                 "전설의필사본", "봉인된연구일지", "잃어버린원전",
                 "미발간원고", "절판서", "유고집", "비공개연구노트", "초판본",
                 "학회발표자료", "개인소장필사본", "미공개강의록", "기밀연구자료", "학파비전서",
                 "원본논문" -> BookType.ANCIENT_BOOK;
            case "문제집", "워크북", "요약노트", "문제해설집", "정석", "응용서",
                 "문제집 해설" -> BookType.WORKBOOK;
            default -> BookType.TEXTBOOK;
        };
    }
}
