package com.third_quadrant.intelligentknowledge.client.gui;

import com.third_quadrant.intelligentknowledge.attachment.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

// 클라이언트 전용 HUD. 서버(Dedicated Server)에서는 로드되지 않는다.
// @EventBusSubscriber(CLIENT)로 등록하면 클라이언트에서만 이벤트를 받는다.
// 1.21.1부터 bus 속성은 무시된다. IModBusEvent(RegisterGuiLayersEvent)면 모드 버스로 자동 등록된다.
@EventBusSubscriber(modid = "intelligentknowledge", value = Dist.CLIENT)
public class ClientKnowledgeHud {
    // HUD 레이어의 고유 ID. 같은 ID로 레이어를 여러 번 등록하면 충돌하므로 한 번만 쓴다.
    private static final ResourceLocation HUD_ID =
            ResourceLocation.fromNamespaceAndPath("intelligentknowledge", "knowledge_hud");

    // 아이콘 원본(16x16) 텍스처 크기. 렌더링 좌표 계산 기준이 된다.
    private static final int ICON_SIZE = 16;
    // 아이콘 축소 배율: 16 * 0.4 = 6.4px로 표시된다. (기존 크기의 반 이하)
    private static final float ICON_SCALE = 0.4F;
    // 화면 오른쪽/아래에서 띄울 여백(px).
    private static final int MARGIN = 4;
    // 숫자와 아이콘 사이 간격(px).
    private static final int TEXT_GAP = 3;
    // 숫자 색상.
    private static final int TEXT_COLOR = 0xd4d4d4;

    // RegisterGuiLayersEvent = 네오포지가 HUD 레이어를 등록하는 시점에 모드 버스에서 발생시켜 주는 이벤트.
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // registerAboveAll: 바닐라 HUD 레이어들 위에 얹히는 레이어로 등록한다.
        // renderHud는 LayeredDraw.Layer (render(GuiGraphics, DeltaTracker)) 형태의 메서드 참조.
        event.registerAboveAll(HUD_ID, ClientKnowledgeHud::renderHud);
    }

    // 매 프레임 화면에 그려지는 메서드.
    private static void renderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();

        // 월드에 진입하지 않았거나, F1으로 HUD가 숨겨진 상태면 그리지 않는다.
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        // 클라이언트 플레이어에게 동기화된 지식 조각 개수를 읽는다. (ModAttachments에 .sync() 덕분에 최신값)
        int count = mc.player.getData(ModAttachments.STONE_KNOWLEDGE_SHARD);
        String text = String.valueOf(count);

        // 축소된 아이콘의 실제 화면 크기(px).
        int iconW = (int) (ICON_SIZE * ICON_SCALE);

        // 화면 오른쪽 아래 기준 좌표: (너비 - 아이콘 - 여백, 높이 - 아이콘 - 여백)
        int x = mc.getWindow().getGuiScaledWidth() - iconW - MARGIN;
        int y = mc.getWindow().getGuiScaledHeight() - iconW - MARGIN;

        // 숫자를 아이콘 왼쪽에 오른쪽 정렬로 배치한다.
        // 글자 폭(font.width)으로 시작 좌표를 계산하므로 자릿수가 늘어나도 화면 밖으로 안 나간다.
        int textWidth = mc.font.width(text);
        int textX = x - TEXT_GAP - textWidth;
        int textY = y + (iconW - mc.font.lineHeight) / 2;

        // 아이콘을 축소해서 그린다. (pose를 scale한 상태에서 0,0에 16x16 아이템을 렌더 → 화면엔 iconW 크기로 보임)
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(ICON_SCALE, ICON_SCALE, 1);
        guiGraphics.renderItem(new ItemStack(Blocks.STONE), 0, 0);
        guiGraphics.pose().popPose();

        // 아이콘 왼쪽에 숫자를 그린다.
        guiGraphics.drawString(mc.font, Component.literal(text), textX, textY, TEXT_COLOR, true);
    }
}
