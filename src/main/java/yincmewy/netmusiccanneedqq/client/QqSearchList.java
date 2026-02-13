package yincmewy.netmusiccanneedqq.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import yincmewy.netmusiccanneedqq.qq.QqSearchResult;

import java.util.List;

public class QqSearchList extends ObjectSelectionList<QqSearchList.Entry> {
    private final QqSearchScreen screen;

    public QqSearchList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight, QqSearchScreen screen) {
        super(minecraft, width, height, top, itemHeight);
        this.screen = screen;
        this.updateSizeAndPosition(width, bottom - top, top);
    }

    public void setResults(List<QqSearchResult> results) {
        this.clearEntries();
        if (results == null) {
            return;
        }
        for (QqSearchResult result : results) {
            this.addEntry(new Entry(result, screen));
        }
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getRight() - 6;
    }

    @Override
    public int getRowWidth() {
        return this.getWidth() - 12;
    }

    @Override
    protected void renderListBackground(GuiGraphics graphics) {
    }

    @Override
    protected void renderListSeparators(GuiGraphics graphics) {
    }

    public static class Entry extends ObjectSelectionList.Entry<Entry> {
        private final QqSearchResult result;
        private final QqSearchScreen screen;

        public Entry(QqSearchResult result, QqSearchScreen screen) {
            this.result = result;
            this.screen = screen;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int color = hovered ? 0xFFFFFF : 0xE0E0E0;
            int pos = x + 4;
            if (result.isVip()) pos = graphics.drawString(screen.getFontRenderer(), "[VIP]", pos, y + 4, 0xC10225, false) + 2;
            graphics.drawString(screen.getFontRenderer(), result.getDisplayText(), pos, y + 4, color, false);

        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                screen.acceptResult(result);
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal(result.getDisplayText());
        }
    }
}
