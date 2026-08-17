package com.third_quadrant.intelligentknowledge.registry;

import com.third_quadrant.intelligentknowledge.item.NoteItem;
import com.third_quadrant.intelligentknowledge.knowledge.common.RandomKnowledgeBookItem;
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
    public static final DeferredItem<Item> GEOLOGIST_CERTIFICATE =
            ITEMS.register("geologist_certificate", () -> new Item(new Item.Properties()));

    // 학위증명서 (지식 마일스톤 달성 시 자동 지급).
    public static final DeferredItem<Item> BACHELOR_DIPLOMA =
            ITEMS.register("bachelor_diploma", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MASTER_DIPLOMA =
            ITEMS.register("master_diploma", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PHD_DIPLOMA =
            ITEMS.register("phd_diploma", () -> new Item(new Item.Properties()));

    // 메모장. stacksTo(16), 내구도 없음 (DataComponent로 페이지 추적).
    public static final DeferredItem<Item> NOTE =
            ITEMS.register("note", () -> new NoteItem(new Item.Properties().stacksTo(16)));

    // 지식 책 (랜덤). stacksTo(1): 책은 서로 겹치지 않음.
    private static final Item.Properties BOOK_PROPS = new Item.Properties().stacksTo(1);

    public static final DeferredItem<RandomKnowledgeBookItem> RANDOM_BOOK_ENTRY =
            ITEMS.register("random_book_entry",
                    () -> new RandomKnowledgeBookItem(BOOK_PROPS, 0, 299, "입문"));
    public static final DeferredItem<RandomKnowledgeBookItem> RANDOM_BOOK_BACHELOR =
            ITEMS.register("random_book_bachelor",
                    () -> new RandomKnowledgeBookItem(BOOK_PROPS, 300, 499, "학사"));
    public static final DeferredItem<RandomKnowledgeBookItem> RANDOM_BOOK_MASTER =
            ITEMS.register("random_book_master",
                    () -> new RandomKnowledgeBookItem(BOOK_PROPS, 500, 999, "석사"));
    public static final DeferredItem<RandomKnowledgeBookItem> RANDOM_BOOK_PHD =
            ITEMS.register("random_book_phd",
                    () -> new RandomKnowledgeBookItem(BOOK_PROPS, 1000, 1500, "박사"));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
