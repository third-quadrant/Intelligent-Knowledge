package com.third_quadrant.intelligentknowledge.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    private static final String MOD_ID = "intelligentknowledge";

    // createItems(): 아이템 전용 DeferredRegister.
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    // 분석기 블록을 손에 들고 설치할 수 있게 해주는 블록 아이템.
    public static final DeferredItem<BlockItem> ROCK_ANALYZER =
            ITEMS.register("rock_analyzer", key -> new BlockItem(ModBlocks.ROCK_ANALYZER.get(), new Item.Properties()));

    // 암석학 학사 자격증. stone_300(암석학 학사) 달성 시 지급되며, 분석기 조합 재료로 쓰인다.
    // (조합에서 소모되어 "학사 미만은 조합 자체가 불가능"하도록 하는 게이트 역할)
    public static final DeferredItem<Item> GEOLOGIST_CERTIFICATE =
            ITEMS.register("geologist_certificate", () -> new Item(new Item.Properties()));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
