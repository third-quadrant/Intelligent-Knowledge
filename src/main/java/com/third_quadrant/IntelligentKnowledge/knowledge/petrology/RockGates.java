package com.third_quadrant.intelligentknowledge.knowledge.petrology;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

// 발전과제 달성 여부를 이용한 자격 레벨 판정. (서버에서만 호출)
public class RockGates {

    // 암석학 학사(stone_300) 달성 여부. 분석기의 사용·제작 게이트.
    public static boolean hasBachelor(ServerPlayer player) {
        return hasAdvancement(player, "knowledge/stone_300");
    }

    // 특정 발전과제가 달성 상태인지 판정한다.
    private static boolean hasAdvancement(ServerPlayer player, String path) {
        // 서버의 발전과제 매니저에서 해당 발전과제를 찾는다. (리소스 로드 안 됐으면 null)
        AdvancementHolder holder = player.server.getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath("intelligentknowledge", path));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
