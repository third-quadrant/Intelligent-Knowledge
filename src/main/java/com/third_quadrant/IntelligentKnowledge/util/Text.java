package com.third_quadrant.intelligentknowledge.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class Text {

    // 기본 RGB
    public static MutableComponent rgb(String text, int color) {
        return Component.literal(text)
                .withStyle(style -> style.withColor(color));
    }

    // 기본 색상
    public static MutableComponent color(String text, net.minecraft.ChatFormatting color) {
        return Component.literal(text)
                .withStyle(color);
    }

    // 볼드
    public static MutableComponent bold(String text) {
        return Component.literal(text)
                .withStyle(style -> style.withBold(true));
    }

    // 기울임
    public static MutableComponent italic(String text) {
        return Component.literal(text)
                .withStyle(style -> style.withItalic(true));
    }

    // 밑줄
    public static MutableComponent underline(String text) {
        return Component.literal(text)
                .withStyle(style -> style.withUnderlined(true));
    }

    // 취소선
    public static MutableComponent strike(String text) {
        return Component.literal(text)
                .withStyle(style -> style.withStrikethrough(true));
    }

    // 난독화
    public static MutableComponent obfuscated(String text) {
        return Component.literal(text)
                .withStyle(style -> style.withObfuscated(true));
    }

    // 글자색 + 볼드
    public static MutableComponent bold(String text, int color) {
        return rgb(text, color)
                .withStyle(style -> style.withBold(true));
    }

    // 글자색 + 기울임
    public static MutableComponent italic(String text, int color) {
        return rgb(text, color)
                .withStyle(style -> style.withItalic(true));
    }

    // 글자색 + 밑줄
    public static MutableComponent underline(String text, int color) {
        return rgb(text, color)
                .withStyle(style -> style.withUnderlined(true));
    }

    // 글자색 + 취소선
    public static MutableComponent strike(String text, int color) {
        return rgb(text, color)
                .withStyle(style -> style.withStrikethrough(true));
    }

    // 글자색 + 난독화
    public static MutableComponent obfuscated(String text, int color) {
        return rgb(text, color)
                .withStyle(style -> style.withObfuscated(true));
    }
}