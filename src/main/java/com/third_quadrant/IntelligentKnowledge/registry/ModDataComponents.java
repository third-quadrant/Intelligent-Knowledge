package com.third_quadrant.intelligentknowledge.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.component.DataComponentType;

public class ModDataComponents {
    private static final String MOD_ID = "intelligentknowledge";

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);

    // 랜덤 지식책의 확인된 속성을 저장하는 컴포넌트.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> RANDOM_BOOK_DATA =
            DATA_COMPONENTS.register("random_book_data", () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .build());

    // 메모장 페이지 진행도 (0~200).
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NOTE_PAGES =
            DATA_COMPONENTS.register("note_pages", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .build());

    public static void register(IEventBus modBus) {
        DATA_COMPONENTS.register(modBus);
    }
}
