package com.third_quadrant.intelligentknowledge.util;

import net.minecraft.util.RandomSource;

public class Chance {
    public static boolean chance(RandomSource random, float percent) {
        return random.nextFloat() * 100 < percent;
    }

    public static boolean chanceBlock(RandomSource random, int count) {
        if (count <= 99) {
            return chance(random, 10);
        } else if (count <= 299) {
            return chance(random, 5);
        } else if (count <= 499) {
            return chance(random, 3);
        } else if (count <= 999) {
            return chance(random, 1);
        } else {
            return chance(random, 0.5F);
        }
    }
}
