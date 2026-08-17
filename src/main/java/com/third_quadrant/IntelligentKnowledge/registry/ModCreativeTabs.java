package com.third_quadrant.intelligentknowledge.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    private static final String MOD_ID = "intelligentknowledge";

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    // 이 모드의 아이템을 한데 모아 보여주는 크리에이티브 탭.
    public static final Supplier<CreativeModeTab> INTELLIGENT_KNOWLEDGE = TABS.register("intelligent_knowledge", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.intelligentknowledge"))
            .icon(() -> new ItemStack(ModBlocks.ROCK_ANALYZER.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModBlocks.ROCK_ANALYZER.get());
                output.accept(ModItems.GEOLOGIST_CERTIFICATE.get());
                output.accept(ModItems.NOTE.get());
                // 학위증명서.
                output.accept(ModItems.BACHELOR_DIPLOMA.get());
                output.accept(ModItems.MASTER_DIPLOMA.get());
                output.accept(ModItems.PHD_DIPLOMA.get());
                // 미확인 지식책 (티어별).
                output.accept(ModItems.RANDOM_BOOK_ENTRY.get());
                output.accept(ModItems.RANDOM_BOOK_BACHELOR.get());
                output.accept(ModItems.RANDOM_BOOK_MASTER.get());
                output.accept(ModItems.RANDOM_BOOK_PHD.get());
            })
            .build());

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
