package yincmewy.netmusiccanneedqq.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yincmewy.netmusiccanneedqq.data.SongInfoData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class PhoneSongList extends ObjectSelectionList<PhoneSongList.Entry> {
    private final Consumer<SongInfoData> onBurn;
    private final Consumer<SongInfoData> onAddToPlaylist;
    private final List<SongInfoData> songs = new ArrayList<>();
    private int listWidth;
    private boolean multiMode;

    public PhoneSongList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight,
                         Consumer<SongInfoData> onBurn, Consumer<SongInfoData> onAddToPlaylist) {
        super(mc, width, height, top, bottom, itemHeight);
        this.listWidth = width;
        this.onBurn = onBurn;
        this.onAddToPlaylist = onAddToPlaylist;
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
    }

    public void setLeftPos(int x) {
        this.x0 = x;
        this.x1 = x + this.listWidth;
    }

    @Override
    public int getRowWidth() {
        return this.listWidth - 6;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.x1 - 6;
    }

    public void setSongs(List<SongInfoData> newSongs, boolean multi) {
        this.songs.clear();
        this.songs.addAll(newSongs);
        this.multiMode = multi;
        this.clearEntries();
        for (SongInfoData song : newSongs) {
            this.addEntry(new Entry(song));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class Entry extends ObjectSelectionList.Entry<Entry> {
        private final SongInfoData song;

        public Entry(SongInfoData song) {
            this.song = song;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            if (hovering) {
                graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, 0x30FFFFFF);
            }

            String display = song.songName;
            if (!song.artists.isEmpty()) {
                display += " - " + String.join("/", song.artists);
            }
            if (song.vip) {
                display = "\u00A7c[VIP]\u00A7r " + display;
            }

            int maxTextWidth = width - 60;
            String trimmed = PhoneSongList.this.minecraft.font.plainSubstrByWidth(display, maxTextWidth);
            if (trimmed.length() < display.length()) {
                trimmed += "...";
            }
            graphics.drawString(PhoneSongList.this.minecraft.font, trimmed, left + 2, top + 3, 0xFFFFFF, false);

            String actionText = multiMode ? "[\u6DFB\u52A0]" : "[\u523B\u5F55]";
            int actionWidth = PhoneSongList.this.minecraft.font.width(actionText);
            int actionX = left + width - actionWidth - 2;

            boolean actionHover = mouseX >= actionX && mouseX <= actionX + actionWidth
                    && mouseY >= top && mouseY <= top + height;
            int actionColor = actionHover ? 0x55FF55 : 0x00CC00;
            graphics.drawString(PhoneSongList.this.minecraft.font, actionText, actionX, top + 3, actionColor, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                if (multiMode) {
                    onAddToPlaylist.accept(song);
                } else {
                    onBurn.accept(song);
                }
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            return Component.literal(song.songName);
        }

        public SongInfoData getSong() {
            return song;
        }
    }
}
