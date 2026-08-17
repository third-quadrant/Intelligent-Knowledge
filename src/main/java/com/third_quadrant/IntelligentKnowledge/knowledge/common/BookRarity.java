package com.third_quadrant.intelligentknowledge.knowledge.common;

import net.minecraft.ChatFormatting;

// 책의 희귀도 분류.
public enum BookRarity {
    COMMON("일반", "Common", 0x808080, ChatFormatting.GRAY),
    UNCOMMON("고급", "Uncommon", 0x55FF55, ChatFormatting.GREEN),
    RARE("희귀", "Rare", 0x5555FF, ChatFormatting.BLUE),
    EPIC("매우 희귀", "Epic", 0xAA00AA, ChatFormatting.DARK_PURPLE),
    LEGENDARY("전설", "Legendary", 0xFFAA00, ChatFormatting.GOLD);

    private final String displayNameKo;
    private final int color;
    private final ChatFormatting chatFormatting;

    BookRarity(String displayNameKo, String displayNameEn, int color, ChatFormatting chatFormatting) {
        this.displayNameKo = displayNameKo;
        this.color = color;
        this.chatFormatting = chatFormatting;
    }

    public String getDisplayNameKo() {
        return displayNameKo;
    }

    public int getColor() {
        return color;
    }

    public ChatFormatting getChatFormatting() {
        return chatFormatting;
    }
}
