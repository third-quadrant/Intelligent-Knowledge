package com.third_quadrant.intelligentknowledge.client;

import com.third_quadrant.intelligentknowledge.client.gui.RockAnalyzerScreen;
import com.third_quadrant.intelligentknowledge.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// 클라이언트 전용: 등록한 메뉴 타입(RockAnalyzerMenu)에 맞는 화면(RockAnalyzerScreen)을 연결한다.
@EventBusSubscriber(modid = "intelligentknowledge", value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ROCK_ANALYZER.get(), RockAnalyzerScreen::new);
    }
}
