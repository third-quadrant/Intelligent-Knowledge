package com.third_quadrant.intelligentknowledge.client.gui;

import com.third_quadrant.intelligentknowledge.knowledge.common.KnowledgeBookDefinition;
import com.third_quadrant.intelligentknowledge.knowledge.common.KnowledgeBookMenu;
import com.third_quadrant.intelligentknowledge.knowledge.common.KnowledgeRegistry;
import com.third_quadrant.intelligentknowledge.knowledge.common.KnowledgeTier;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KnowledgeBookScreen extends AbstractContainerScreen<KnowledgeBookMenu> {
    private static final ResourceLocation BG_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/gui/book.png");

    private static final int BOOK_W = 192;
    private static final int BOOK_H = 192;

    private static final int PAD_LEFT = 40;
    private static final int PAD_RIGHT = 18;
    private static final int PAD_TOP = 18;
    private static final int CONTENT_X = PAD_LEFT;
    private static final int CONTENT_W = BOOK_W - PAD_LEFT - PAD_RIGHT; // 156

    private static final int LINE_H = 9;

    // 게이지: 8칸으로 축소 (글자 폭 ~6px × 8 = 48px).
    private static final int BAR_LEN = 8;
    private static final char FILLED = '\u2588';
    private static final char EMPTY = '\u2591';

    // 색상.
    private static final int COL_TITLE = 0x2D1B0E;
    private static final int COL_LABEL = 0x6B4C2A;
    private static final int COL_VALUE = 0x3B2507;
    private static final int COL_SEP = 0xA08B6E;
    private static final int COL_STUDY = 0x996B1A;
    private static final int COL_DARK = 0x3B2507;
    private static final int COL_TIME = 0x5B4020;

    // 학문 아이콘 (임시: stone). 나중에 학문별 아이템으로 교체 가능.
    private static final ItemStack ICON_ITEM = new ItemStack(net.minecraft.world.item.Items.STONE);
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;

    private Button studyButton;
    private Button takeButton;

    public KnowledgeBookScreen(KnowledgeBookMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BOOK_W;
        this.imageHeight = BOOK_H;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.leftPos + BOOK_W / 2;
        int btnY = this.topPos + BOOK_H - 30;
        int btnW = 60;
        int gap = 8;

        this.studyButton = this.addRenderableWidget(
                Button.builder(Component.literal("공부"), b -> onStudyClicked())
                        .bounds(centerX - btnW - gap / 2, btnY, btnW, 20).build());
        this.takeButton = this.addRenderableWidget(
                Button.builder(Component.literal("가져가기"), b -> onTakeClicked())
                        .bounds(centerX + gap / 2, btnY, btnW, 20).build());

        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean studying = this.menu.isStudying();
        this.studyButton.active = !studying;
        this.studyButton.setMessage(studying ? Component.literal("공부 중...") : Component.literal("공부"));
        // 가져가기 버튼은 항상 활성화.
        this.takeButton.active = true;
    }

    private void onStudyClicked() {
        // 서버 응답 전에 즉시 버튼 상태 변경 (한 번 클릭으로 반응).
        this.studyButton.active = false;
        this.studyButton.setMessage(Component.literal("공부 중..."));
        this.minecraft.gameMode.handleInventoryButtonClick(
                this.menu.containerId, KnowledgeBookMenu.BUTTON_STUDY);
    }

    private void onTakeClicked() {
        this.minecraft.gameMode.handleInventoryButtonClick(
                this.menu.containerId, KnowledgeBookMenu.BUTTON_TAKE);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        this.renderBg(g, partialTick, mouseX, mouseY);
        for (var renderable : this.renderables) {
            renderable.render(g, mouseX, mouseY, partialTick);
        }
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int bx = this.leftPos;
        int by = this.topPos;

        g.blit(BG_LOCATION, bx, by, 0, 0, BOOK_W, BOOK_H);

        KnowledgeBookDefinition def = KnowledgeRegistry.get(menu.getBookId());
        if (def == null) def = KnowledgeRegistry.getByProperties(
                menu.getBookDifficulty(), menu.getMaxShards(), menu.getBaseReward());
        if (def == null) def = KnowledgeRegistry.getByDifficulty(menu.getBookDifficulty());
        if (def == null) return;

        // 클리핑 영역.
        g.enableScissor(bx + CONTENT_X, by + PAD_TOP,
                bx + CONTENT_X + CONTENT_W, by + BOOK_H - 34);

        int cx = bx + CONTENT_X;
        int cy = by + PAD_TOP;

        // 학문 아이콘 (제목 왼쪽).
        int iconX = cx;
        int iconY = cy;
        g.renderItem(ICON_ITEM, iconX, iconY);
        g.renderItemDecorations(this.font, ICON_ITEM, iconX, iconY);

        // 제목 (아이콘 오른쪽에서 중앙정렬).
        int titleColor = def.getTitleColor();
        int titleAreaX = cx + ICON_SIZE + ICON_GAP;
        int titleAreaW = CONTENT_W - ICON_SIZE - ICON_GAP;
        cy = drawWrapped(g, Component.literal(def.title())
                .withStyle(Style.EMPTY.withBold(true).withColor(titleColor)), titleAreaX, cy, titleAreaW, true);

        // 종류 + 희귀도 (제목 아래).
        String typeRarity = def.bookType().getDisplayNameKo() + " / " + def.bookRarity().getDisplayNameKo();
        int rarityColor = def.bookRarity().getColor();
        g.drawString(this.font, typeRarity, cx + (CONTENT_W - this.font.width(typeRarity)) / 2,
                cy, rarityColor, false);
        cy += LINE_H;

        // 구분선.
        cy += 2;
        g.hLine(cx, cx + CONTENT_W, cy, COL_SEP);
        cy += 3;

        // 정보.
        cy = drawInfoLine(g, "[저자] ", def.author(), cx, cy);
        cy = drawInfoLine(g, "[학문] ", def.field(), cx, cy);
        cy = drawInfoLine(g, "[난이도] ",
                def.getTier().getDisplayName() + " (" + def.difficulty() + ")", cx, cy);

        // 구분선.
        cy += 2;
        g.hLine(cx, cx + CONTENT_W, cy, COL_SEP);
        cy += 3;

        // 이해도.
        cy = drawProgress(g, menu.getBookProgress(), menu.getMaxShards(), cx, cy);

        // 공부 중일 때: 진행 바 + 남은 시간.
        // 공부 아닐 때: 예상 시간 표시.
        cy += 2;
        if (menu.isStudying()) {
            cy = drawStudyActive(g, menu.getStudyPercentFromSlot(), cx, cy);
        } else {
            cy = drawEstimatedTime(g, def, cx, cy);
        }

        g.disableScissor();
    }

    // ─── 텍스트 자동 줄바꿈 (라벨 중앙정렬 지원) ───
    private int drawWrapped(GuiGraphics g, Component text, int cx, int y, int maxW, boolean centered) {
        List<FormattedCharSequence> lines = this.font.split(text, maxW);
        int color = text.getStyle().getColor() != null ? text.getStyle().getColor().getValue() : 0xFFFFFF;
        for (FormattedCharSequence line : lines) {
            if (centered) {
                int w = this.font.width(line);
                g.drawString(this.font, line, cx + (maxW - w) / 2, y, color, false);
            } else {
                g.drawString(this.font, line, cx, y, color, false);
            }
            y += LINE_H;
        }
        return y;
    }

    // ─── [라벨] 값 형태 ───
    private int drawInfoLine(GuiGraphics g, String label, String value, int cx, int y) {
        g.drawString(this.font, label, cx, y, COL_LABEL, false);
        int labelW = this.font.width(label);
        int valueMaxW = CONTENT_W - labelW;
        List<FormattedCharSequence> valLines = this.font.split(
                Component.literal(value).withStyle(Style.EMPTY.withColor(COL_VALUE)), valueMaxW);
        if (valLines.isEmpty()) { y += LINE_H; return y; }
        g.drawString(this.font, valLines.get(0), cx + labelW, y, COL_VALUE, false);
        y += LINE_H;
        for (int i = 1; i < valLines.size(); i++) {
            g.drawString(this.font, valLines.get(i), cx, y, COL_VALUE, false);
            y += LINE_H;
        }
        return y;
    }

    // ─── 이해도: 라벨 + 게이지 + 수치 ───
    private int drawProgress(GuiGraphics g, int cur, int max, int cx, int y) {
        g.drawString(this.font, "이해도", cx, y, COL_LABEL, false);
        y += LINE_H;

        // 게이지: "████░░░░ 50%"
        double pct = max > 0 ? (double) cur / max : 0;
        int filled = (int) Math.round(pct * BAR_LEN);
        int empty = BAR_LEN - filled;
        String bar = String.valueOf(FILLED).repeat(Math.max(0, filled))
                + String.valueOf(EMPTY).repeat(Math.max(0, empty));
        int pctInt = (int) Math.round(pct * 100);
        g.drawString(this.font, bar + " " + pctInt + "%", cx, y, COL_DARK, false);
        y += LINE_H;

        g.drawString(this.font, cur + " / " + max, cx, y, COL_VALUE, false);
        y += LINE_H;
        return y;
    }

    // ─── 공부 진행 중: 라벨 + 바 + 남은 시간 + 획득 조각 (4줄) ───
    private int drawStudyActive(GuiGraphics g, int pct, int cx, int y) {
        // 1줄: "공부 진행"
        g.drawString(this.font, "공부 진행", cx, y, COL_LABEL, false);
        y += LINE_H;

        // 2줄: "████░░░░ 66%"
        int sf = Math.round(pct * BAR_LEN / 100.0f);
        int se = BAR_LEN - sf;
        String bar = String.valueOf(FILLED).repeat(Math.max(0, sf))
                + String.valueOf(EMPTY).repeat(Math.max(0, se));
        g.drawString(this.font, bar + " " + pct + "%", cx, y, COL_STUDY, false);
        y += LINE_H;

        // 3줄: 획득한 지식 조각.
        int granted = menu.getGrantedShards();
        g.drawString(this.font, "획득 조각: +" + granted, cx, y, COL_TIME, false);
        y += LINE_H;

        // 4줄: 남은 시간 계산.
        int gap = menu.getBookDifficulty() - menu.getPlayerKnowledge();
        if (gap > 0) {
            int totalTicks = KnowledgeTier.computeReadTime(gap);
            int remainTicks = (int) (totalTicks * (100L - pct) / 100L);
            String time = formatTime(remainTicks);
            g.drawString(this.font, "남은 시간: " + time, cx, y, COL_TIME, false);
            y += LINE_H;
        }
        return y;
    }

    // ─── 공부 전: 예상 독서 시간 + 예상 조각 표시 ───
    private int drawEstimatedTime(GuiGraphics g, KnowledgeBookDefinition def, int cx, int y) {
        // 이미 maxShards 도달 시.
        if (menu.getBookProgress() >= menu.getMaxShards()) {
            g.drawString(this.font, "이 책은 이미 읽었습니다", cx, y, COL_LABEL, false);
            return y + LINE_H;
        }
        net.minecraft.world.entity.player.Player player = this.minecraft.player;
        int gap = player != null ? menu.getEffectiveGap(player, def.difficulty())
                : def.difficulty() - menu.getPlayerKnowledge();

        // 예상 시간.
        int totalTicks = KnowledgeTier.computeReadTime(gap);
        String time = formatTime(totalTicks);
        g.drawString(this.font, "예상 시간: " + time, cx, y, COL_TIME, false);
        y += LINE_H;

        // 예상 획득 조각.
        int expectedReward = KnowledgeTier.computeReward(gap, def.baseReward(),
                menu.getBookProgress(), menu.getMaxShards());
        g.drawString(this.font, "예상 조각: +" + expectedReward, cx, y, COL_TIME, false);
        return y + LINE_H;
    }

    // ─── 틱 → "X분 X초" ───
    private static String formatTime(int ticks) {
        int totalSec = ticks / 20;
        if (totalSec < 60) {
            return totalSec + "초";
        }
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return sec > 0 ? min + "분 " + sec + "초" : min + "분";
    }
}
