package yincmewy.netmusiccanneedqq.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import yincmewy.netmusiccanneedqq.qq.QqMusicUtils;
import yincmewy.netmusiccanneedqq.qq.QqSearchResult;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class QqSearchScreen extends Screen {
    private static final int SEARCH_HEIGHT = 16;
    private static final int SEARCH_BUTTON_WIDTH = 56;
    private static final int LIST_ITEM_HEIGHT = 20;
    private static final int LIST_TOP_PADDING = 8;

    private final Screen parent;
    private final QqSearchTarget target;
    private final String initialQuery;

    private EditBox searchBox;
    private Button searchButton;
    private QqSearchList list;
    private Component status = Component.empty();
    private boolean searching;

    public QqSearchScreen(Screen parent, String initialQuery) {
        super(Component.literal("QQ Search"));
        this.parent = parent;
        this.target = parent instanceof QqSearchTarget ? (QqSearchTarget) parent : null;
        this.initialQuery = initialQuery == null ? "" : initialQuery;
    }

    @Override
    protected void init() {
        int boxWidth = Math.min(200, this.width - SEARCH_BUTTON_WIDTH - 24);
        int boxX = (this.width - (boxWidth + SEARCH_BUTTON_WIDTH + 4)) / 2;
        int boxY = 20;
        this.searchBox = new EditBox(this.font, boxX, boxY, boxWidth, SEARCH_HEIGHT, Component.literal("Search Box"));
        this.searchBox.setValue(initialQuery);
        this.searchBox.setBordered(true);
        this.addRenderableWidget(this.searchBox);

        this.searchButton = Button.builder(Component.literal("\u641C\u7D22"), button -> runSearch())
                .pos(boxX + boxWidth + 4, boxY)
                .size(SEARCH_BUTTON_WIDTH, SEARCH_HEIGHT)
                .build();
        this.addRenderableWidget(this.searchButton);

        int listTop = boxY + SEARCH_HEIGHT + LIST_TOP_PADDING;
        int listBottom = this.height - 20;
        this.list = new QqSearchList(this.minecraft, this.width, this.height, listTop, listBottom, LIST_ITEM_HEIGHT, this);
        this.addRenderableWidget(this.list);

        if (!this.initialQuery.isBlank()) {
            runSearch();
        }
    }

    @Override
    public void tick() {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!this.status.getString().isEmpty()) {
            graphics.drawCenteredString(this.font, this.status, this.width / 2, 52, 0xCCCCCC);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            runSearch();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    public void acceptResult(QqSearchResult result) {
        if (target != null) {
            target.netmusiccanneedqq$applySearchResult(result.getId());
        }
        Minecraft.getInstance().setScreen(parent);
    }

    public net.minecraft.client.gui.Font getFontRenderer() {
        return this.font;
    }

    private void runSearch() {
        if (this.searchBox == null) {
            return;
        }
        String query = this.searchBox.getValue().trim();
        if (query.isEmpty()) {
            this.status = Component.literal("\u8F93\u5165\u6B4C\u66F2\u540D\u8FDB\u884C\u641C\u7D22");
            if (this.list != null) {
                this.list.setResults(Collections.emptyList());
            }
            return;
        }
        if (this.searching) {
            return;
        }
        this.searching = true;
        this.status = Component.literal("\u641C\u7D22\u4E2D...");
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return QqMusicUtils.search(query);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, Util.backgroundExecutor())
                .whenComplete((results, error) -> Minecraft.getInstance().execute(() -> applyResults(results, error)));
    }

    private void applyResults(List<QqSearchResult> results, Throwable error) {
        this.searching = false;
        if (error != null) {
            this.status = Component.literal("\u641C\u7D22\u5931\u8D25");
            if (this.list != null) {
                this.list.setResults(Collections.emptyList());
            }
            return;
        }
        if (this.list != null) {
            this.list.setResults(results);
        }
        if (results == null || results.isEmpty()) {
            this.status = Component.literal("\u65E0\u7ED3\u679C");
        } else {
            this.status = Component.empty();
        }
    }
}
