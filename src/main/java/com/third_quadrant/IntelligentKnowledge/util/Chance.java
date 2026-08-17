package com.third_quadrant.intelligentknowledge.util;

import com.third_quadrant.intelligentknowledge.knowledge.common.KnowledgeTier;
import net.minecraft.util.RandomSource;

public class Chance {
    public static boolean chance(RandomSource random, float percent) {
        return random.nextFloat() * 100 < percent;
    }

    // KnowledgeTier.getMiningChance()에서 단계별 확률을 관리.
    public static boolean chanceBlock(RandomSource random, int count) {
        return chance(random, KnowledgeTier.getMiningChance(count));
    }
}
