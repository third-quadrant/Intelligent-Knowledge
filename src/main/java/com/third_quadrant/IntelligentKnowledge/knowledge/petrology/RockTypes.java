package com.third_quadrant.intelligentknowledge.knowledge.petrology;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// 암석 분석기에서 "암석"으로 취급할 아이템 목록을 관리하는 유틸.
// 채굴 감지(BASE_STONE_OVERWORLD)와 동일한 돌 계열 + 일반 채굴 시 나오는 깬돌 계열을 모두 포함한다.
public class RockTypes {

    // 분석기에서 서로 1:1 변환 가능한 암석들. (8종)
    public static final List<Item> ROCK_ITEMS = List.of(
            Items.STONE,
            Items.COBBLESTONE,
            Items.GRANITE,
            Items.DIORITE,
            Items.ANDESITE,
            Items.TUFF,
            Items.DEEPSLATE,
            Items.COBBLED_DEEPSLATE);

    // 해당 아이템이 분석기의 "암석" 입력으로 유효한지 판정한다.
    public static boolean isRock(ItemStack stack) {
        return ROCK_ITEMS.contains(stack.getItem());
    }

    // 입력 암석으로 변환할 수 있는 대상 목록을 만든다. (같은 종류로의 변환은 제외)
    // 반환 리스트는 가변(ArrayList)이어야 한다. 메뉴에서 recipes.clear() 후 다시 채우기 때문이다.
    public static List<ItemStack> conversionTargets(ItemStack input) {
        return ROCK_ITEMS.stream()
                .filter(item -> !item.equals(input.getItem()))
                .map(ItemStack::new)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
}
