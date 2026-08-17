package com.third_quadrant.intelligentknowledge.knowledge.common;

import com.third_quadrant.intelligentknowledge.attachment.ModAttachments;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;

// 지식 책 아이템. 하나의 DeferredItem이 하나의 책 정의를 가진다.
// Lore에 저자/분야/지식수준/이해도를 정보 카드처럼 표시한다.
// difficulty >= 500이면 인챈트 글로우 효과를 적용한다.
public class KnowledgeBookItem extends Item {
    private static final int BAR_LENGTH = 10;
    private static final char FILLED = '\u2588'; // █
    private static final char EMPTY = '\u2591';  // ░

    // 색상 상수.
    private static final int COLOR_CATEGORY = 0x808080; // 카테고리 라벨 (회색)
    private static final int COLOR_VALUE = 0xFFFFFF;      // 값 (흰색)
    private static final int COLOR_BAR_FILLED = 0x55FF55; // 게이지 채워진 부분 (연한 녹색)
    private static final int COLOR_BAR_EMPTY = 0x555555;  // 게이지 빈 부분 (어두운 회색)
    private static final int COLOR_WARNING = 0xFF5555;     // 경고 (빨간색)

    private final String bookId;

    public KnowledgeBookItem(String bookId, Properties properties) {
        super(properties);
        this.bookId = bookId;
    }

    public String getBookId() {
        return bookId;
    }

    public KnowledgeBookDefinition getDefinition() {
        return KnowledgeRegistry.get(bookId);
    }

    // difficulty >= 500이면 인챈트 글로우 효과.
    @Override
    public boolean isFoil(ItemStack stack) {
        KnowledgeBookDefinition def = getDefinition();
        return def != null && def.difficulty() >= 500;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        KnowledgeBookDefinition def = getDefinition();
        if (def == null) return;

        // 제목은 아이템 이름으로 이미 표시되므로 Lore에서 생략.

        // [종류] [희귀도]
        String typeName = def.bookType().getDisplayNameKo();
        String rarityName = def.bookRarity().getDisplayNameKo();
        int rarityColor = def.bookRarity().getColor();
        tooltip.add(Component.literal("")
                .append(Component.literal("[" + typeName + "]").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAA9966))))
                .append(Component.literal(" "))
                .append(Component.literal("[" + rarityName + "]").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rarityColor)))));

        // [저자]
        tooltip.add(Component.literal("")
                .append(Component.literal("[저자] ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_CATEGORY))))
                .append(Component.literal(def.author()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_VALUE)))));

        // [학문]
        tooltip.add(Component.literal("")
                .append(Component.literal("[학문] ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_CATEGORY))))
                .append(Component.literal(def.field()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_VALUE)))));

        // [지식 수준]
        String tierName = def.getTier().getDisplayName();
        tooltip.add(Component.literal("")
                .append(Component.literal("[지식 수준] ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_CATEGORY))))
                .append(Component.literal(tierName + " (" + def.difficulty() + ")").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_VALUE)))));

        // 빈 줄.
        tooltip.add(Component.literal(""));

        // 클라이언트에서만 플레이어 진행도를 읽어 Lore를 그린다.
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        int currentBookShards = player.getData(ModAttachments.BOOK_READ_PROGRESS)
                .getOrDefault(bookId, 0);

        // [이해도]
        tooltip.add(Component.literal("[이해도] ")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_CATEGORY)))
                .append(Component.literal(currentBookShards + " / " + def.maxShards())
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_VALUE)))));

        // 이해도 게이지.
        double progress = def.maxShards() > 0 ? (double) currentBookShards / def.maxShards() : 0;
        int filled = (int) Math.round(progress * BAR_LENGTH);
        int empty = BAR_LENGTH - filled;
        String bar = "§a" + String.valueOf(FILLED).repeat(Math.max(0, filled))
                + "§8" + String.valueOf(EMPTY).repeat(Math.max(0, empty));
        int percent = (int) Math.round(progress * 100);

        tooltip.add(Component.literal(bar + " " + percent + "%"));

        // 수준 이하 보상 안내.
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
}
