package com.third_quadrant.intelligentknowledge.registry;

import com.third_quadrant.intelligentknowledge.knowledge.petrology.RockAnalyzerMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    private static final String MOD_ID = "intelligentknowledge";

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);

    // MenuType = 메뉴(인벤토리 UI) 타입. FeatureFlagSet은 바닐라 석재절단기처럼 VANILLA_SET을 쓴다.
    public static final Supplier<MenuType<RockAnalyzerMenu>> ROCK_ANALYZER =
            MENUS.register("rock_analyzer", () -> new MenuType<>(RockAnalyzerMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
