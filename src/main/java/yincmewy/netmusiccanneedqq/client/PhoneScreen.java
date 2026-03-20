package yincmewy.netmusiccanneedqq.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yincmewy.netmusiccanneedqq.config.ClientConfig;
import yincmewy.netmusiccanneedqq.data.ParsedUrl;
import yincmewy.netmusiccanneedqq.data.SongInfoData;
import yincmewy.netmusiccanneedqq.item.PhoneMenu;
import yincmewy.netmusiccanneedqq.network.NetMusicCanNeedQQNetwork;
import yincmewy.netmusiccanneedqq.network.PhoneAddToPlaylistMessage;
import yincmewy.netmusiccanneedqq.network.PhoneBurnMessage;
import yincmewy.netmusiccanneedqq.qq.QqCredentialManager;
import yincmewy.netmusiccanneedqq.qq.QqLoginService;
import yincmewy.netmusiccanneedqq.qq.QqMusicApi;
import yincmewy.netmusiccanneedqq.qq.QqUrlParser;

import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class PhoneScreen extends AbstractContainerScreen<PhoneMenu> {
    private static final int BG_WIDTH = 220;
    private static final int BG_HEIGHT = 262;

    private EditBox urlInput;
    private Button parseButton;
    private Button tabLinkButton;
    private Button tabLoginButton;
    private PhoneSongList songList;
    private QrCodeRenderer qrRenderer;

    private boolean loginTab;
    private Component statusText = Component.empty();
    private boolean parsing;
    private boolean loggingIn;
    private int pollTimer;
    private boolean waitingBurn;
    private String lastSlotSongUrl = "";
    private int waitingAddCount;
    private int addedCount;

    public PhoneScreen(PhoneMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.qrRenderer = new QrCodeRenderer();

        int cx = this.leftPos;
        int cy = this.topPos;

        this.tabLinkButton = Button.builder(Component.literal("\u94FE\u63A5\u89E3\u6790"), b -> switchTab(false))
                .pos(cx + 6, cy + 6).size(70, 16).build();
        this.tabLoginButton = Button.builder(Component.literal("\u626B\u7801\u767B\u5F55"), b -> switchTab(true))
                .pos(cx + 80, cy + 6).size(70, 16).build();
        this.addRenderableWidget(tabLinkButton);
        this.addRenderableWidget(tabLoginButton);

        int inputY = cy + 28;
        int inputWidth = BG_WIDTH - 56;
        this.urlInput = new EditBox(this.font, cx + 6, inputY, inputWidth, 16, Component.literal("URL"));
        this.urlInput.setMaxLength(512);
        this.urlInput.setBordered(true);
        this.addWidget(this.urlInput);

        this.parseButton = Button.builder(Component.literal("\u89E3\u6790"), b -> doParse())
                .pos(cx + inputWidth + 10, inputY).size(40, 16).build();
        this.addRenderableWidget(this.parseButton);

        int listTop = cy + 60;
        int listBottom = cy + 140;
        this.songList = new PhoneSongList(this.minecraft, BG_WIDTH - 12, this.height,
                listTop, listBottom, 16, this::onBurn, this::onAddToPlaylist);
        this.songList.setLeftPos(cx + 6);
        this.addWidget(this.songList);

        updateTabVisibility();
        updateSlotTracking();
    }

    private void updateSlotTracking() {
        ItemStack slotItem = this.menu.getInputStack();
        if (!slotItem.isEmpty() && yincmewy.netmusiccanneedqq.compat.PlaylistCompat.isPlaylistItem(slotItem)) {
            addedCount = yincmewy.netmusiccanneedqq.compat.PlaylistCompat.getSongCount(slotItem);
        } else {
            addedCount = 0;
        }
    }

    private void switchTab(boolean toLogin) {
        this.loginTab = toLogin;
        updateTabVisibility();
        if (toLogin) {
            updateLoginStatus();
        }
    }

    private void updateTabVisibility() {
        boolean link = !loginTab;
        this.urlInput.visible = link;
        this.parseButton.visible = link;
        this.tabLinkButton.active = loginTab;
        this.tabLoginButton.active = !loginTab;
    }

    private void updateLoginStatus() {
        if (QqCredentialManager.hasValidCredential()) {
            statusText = Component.literal("\u2714 \u5DF2\u767B\u5F55 (musicid: " + QqCredentialManager.getMusicId() + ")");
        } else {
            statusText = Component.literal("\u70B9\u51FB\u4E0B\u65B9\u6309\u94AE\u83B7\u53D6\u4E8C\u7EF4\u7801");
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem() && this.menu.getCarried().isEmpty()) {
            graphics.renderTooltip(this.font, this.hoveredSlot.getItem(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int cx = this.leftPos;
        int cy = this.topPos;

        graphics.fill(cx, cy, cx + BG_WIDTH, cy + BG_HEIGHT, 0xCC222222);
        graphics.renderOutline(cx, cy, BG_WIDTH, BG_HEIGHT, 0xFF555555);
        graphics.drawString(this.font, "Phone", cx + BG_WIDTH - this.font.width("Phone") - 6, cy + 10, 0x666666, false);
        graphics.fill(cx + 1, cy + 24, cx + BG_WIDTH - 1, cy + 25, 0xFF444444);

        if (!loginTab) {
            renderLinkTab(graphics, cx, cy, mouseX, mouseY, partialTick);
        } else {
            renderLoginTab(graphics, cx, cy, mouseX, mouseY);
        }

        drawSlotArea(graphics, cx, cy);
        drawInventoryArea(graphics, cx, cy);
    }

    private void renderLinkTab(GuiGraphics graphics, int cx, int cy, int mouseX, int mouseY, float pt) {
        if (this.urlInput != null) {
            this.urlInput.render(graphics, mouseX, mouseY, pt);
        }

        if (!statusText.getString().isEmpty()) {
            graphics.drawCenteredString(this.font, statusText, cx + BG_WIDTH / 2, cy + 48, 0xCCCCCC);
        }

        if (this.songList != null && !this.songList.children().isEmpty()) {
            this.songList.render(graphics, mouseX, mouseY, pt);
        }
    }

    private void drawSlotArea(GuiGraphics graphics, int cx, int cy) {
        int slotAreaY = cy + 142;
        graphics.fill(cx + 1, slotAreaY, cx + BG_WIDTH - 1, slotAreaY + 1, 0xFF444444);

        int slotX = cx + PhoneMenu.CD_SLOT_X - 1;
        int slotY = cy + PhoneMenu.CD_SLOT_Y - 1;
        graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF333333);
        graphics.renderOutline(slotX, slotY, 18, 18, 0xFF666666);

        graphics.drawString(this.font, "\u653E\u5165\u5531\u7247\u2192", cx + 8, cy + PhoneMenu.CD_SLOT_Y + 4, 0x999999, false);

        int afterSlot = slotX + 22;
        graphics.drawString(this.font, "\u6216\u64AD\u653E\u5217\u8868", afterSlot, cy + PhoneMenu.CD_SLOT_Y + 4, 0x777777, false);
    }

    private void drawInventoryArea(GuiGraphics graphics, int cx, int cy) {
        int sepY = cy + 168;
        graphics.fill(cx + 1, sepY, cx + BG_WIDTH - 1, sepY + 1, 0xFF555555);

        graphics.drawString(this.font, "\u7269\u54C1\u680F", cx + PhoneMenu.INV_X, cy + 172, 0xAAAAAA, false);

        int invLeft = cx + PhoneMenu.INV_X - 1;
        int invTop = cy + PhoneMenu.INV_Y - 1;
        int invRight = invLeft + 9 * 18;
        int invBottom = invTop + 3 * 18;
        graphics.fill(invLeft, invTop, invRight, invBottom, 0x44000000);
        graphics.renderOutline(invLeft, invTop, 9 * 18, 3 * 18, 0xFF444444);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = cx + PhoneMenu.INV_X + col * 18 - 1;
                int sy = cy + PhoneMenu.INV_Y + row * 18 - 1;
                graphics.renderOutline(sx, sy, 18, 18, 0xFF3A3A3A);
            }
        }

        int hotbarTop = cy + PhoneMenu.HOTBAR_Y - 1;
        graphics.fill(invLeft, hotbarTop, invRight, hotbarTop + 18, 0x44000000);
        graphics.renderOutline(invLeft, hotbarTop, 9 * 18, 18, 0xFF444444);
        for (int col = 0; col < 9; col++) {
            int sx = cx + PhoneMenu.INV_X + col * 18 - 1;
            graphics.renderOutline(sx, hotbarTop, 18, 18, 0xFF3A3A3A);
        }
    }

    private void renderLoginTab(GuiGraphics graphics, int cx, int cy, int mouseX, int mouseY) {
        graphics.drawCenteredString(this.font, statusText, cx + BG_WIDTH / 2, cy + 30, 0xFFFFFF);

        if (qrRenderer.isLoaded()) {
            int qrSize = 100;
            int qrX = cx + (BG_WIDTH - qrSize) / 2;
            int qrY = cy + 44;
            qrRenderer.render(graphics, qrX, qrY, qrSize);
        }

        if (!loggingIn && !QqCredentialManager.hasValidCredential()) {
            int btnW = 100;
            int btnX = cx + (BG_WIDTH - btnW) / 2;
            int btnY = qrRenderer.isLoaded() ? cy + 148 : cy + 80;
            boolean hover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + 18;
            graphics.fill(btnX, btnY, btnX + btnW, btnY + 18, hover ? 0xFF446644 : 0xFF333333);
            graphics.renderOutline(btnX, btnY, btnW, 18, 0xFF666666);
            graphics.drawCenteredString(this.font, "\u83B7\u53D6\u4E8C\u7EF4\u7801", btnX + btnW / 2, btnY + 5, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (loginTab && button == 0 && !loggingIn && !QqCredentialManager.hasValidCredential()) {
            int cx = this.leftPos;
            int cy = this.topPos;
            int btnW = 100;
            int btnX = cx + (BG_WIDTH - btnW) / 2;
            int btnY = qrRenderer.isLoaded() ? cy + 148 : cy + 80;
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + 18) {
                startQrLogin();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!loginTab && urlInput != null && urlInput.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                doParse();
                return true;
            }
            urlInput.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (urlInput != null) urlInput.tick();

        if (loggingIn && pollTimer > 0) {
            pollTimer--;
            if (pollTimer == 0) {
                pollLoginStatus();
            }
        }

        if (waitingBurn) {
            ItemStack cd = this.menu.getInputStack();
            if (!cd.isEmpty()) {
                var songInfo = com.github.tartaricacid.netmusic.item.ItemMusicCD.getSongInfo(cd);
                String currentUrl = songInfo != null ? songInfo.songUrl : "";
                if (currentUrl != null && !currentUrl.isBlank() && !currentUrl.equals(lastSlotSongUrl)) {
                    statusText = Component.literal("\u00A7a\u2714 \u523B\u5F55\u5B8C\u6210!");
                    waitingBurn = false;
                }
            }
        }

        if (waitingAddCount > 0) {
            ItemStack playlist = this.menu.getInputStack();
            if (!playlist.isEmpty() && yincmewy.netmusiccanneedqq.compat.PlaylistCompat.isPlaylistItem(playlist)) {
                int currentCount = yincmewy.netmusiccanneedqq.compat.PlaylistCompat.getSongCount(playlist);
                if (currentCount > addedCount) {
                    int diff = currentCount - addedCount;
                    waitingAddCount = Math.max(0, waitingAddCount - diff);
                    addedCount = currentCount;
                    if (waitingAddCount == 0) {
                        statusText = Component.literal("\u00A7a\u2714 \u6DFB\u52A0\u5B8C\u6210! (\u5171 " + currentCount + " \u9996)");
                    }
                }
            }
        }
    }

    private void doParse() {
        if (parsing || urlInput == null) return;
        String input = urlInput.getValue().trim();
        if (input.isEmpty()) {
            statusText = Component.literal("\u8BF7\u8F93\u5165 QQ \u97F3\u4E50\u94FE\u63A5\u6216\u6B4C\u66F2 mid");
            return;
        }
        parsing = true;
        statusText = Component.literal("\u89E3\u6790\u4E2D...");

        net.minecraft.Util.backgroundExecutor().execute(() -> {
            ParsedUrl parsed = QqUrlParser.parse(input);
            if (parsed == null) {
                Minecraft.getInstance().execute(() -> {
                    statusText = Component.literal("\u65E0\u6CD5\u89E3\u6790\u8BE5\u94FE\u63A5");
                    parsing = false;
                });
                return;
            }

            switch (parsed.getType()) {
                case SONG -> QqMusicApi.fetchSongDetail(parsed.getId()).whenComplete((song, err) ->
                        Minecraft.getInstance().execute(() -> {
                            parsing = false;
                            if (err != null || song == null) {
                                statusText = Component.literal("\u83B7\u53D6\u6B4C\u66F2\u4FE1\u606F\u5931\u8D25");
                            } else {
                                statusText = Component.literal("\u5355\u66F2: " + song.songName);
                                songList.setSongs(List.of(song), false);
                            }
                        }));
                case ALBUM -> QqMusicApi.fetchAlbumSongs(parsed.getId()).whenComplete((songs, err) ->
                        Minecraft.getInstance().execute(() -> {
                            parsing = false;
                            applyResults(songs, err, "\u4E13\u8F91");
                        }));
                case PLAYLIST -> QqMusicApi.fetchPlaylistSongs(parsed.getId()).whenComplete((songs, err) ->
                        Minecraft.getInstance().execute(() -> {
                            parsing = false;
                            applyResults(songs, err, "\u6B4C\u5355");
                        }));
            }
        });
    }

    private void applyResults(List<SongInfoData> songs, Throwable err, String typeName) {
        if (err != null || songs == null || songs.isEmpty()) {
            statusText = Component.literal("\u83B7\u53D6" + typeName + "\u5931\u8D25\u6216\u65E0\u7ED3\u679C");
            songList.setSongs(Collections.emptyList(), false);
        } else {
            statusText = Component.literal(typeName + " - \u5171 " + songs.size() + " \u9996");
            songList.setSongs(songs, true);
        }
    }

    private void onBurn(SongInfoData song) {
        handleSongAction(song);
    }

    private void onAddToPlaylist(SongInfoData song) {
        handleSongAction(song);
    }

    private void handleSongAction(SongInfoData song) {
        if (song == null) return;
        String mid = song.songUrl;
        if (mid == null || mid.isBlank()) return;

        ItemStack slotItem = this.menu.getInputStack();
        if (slotItem.isEmpty()) {
            statusText = Component.literal("\u00A7c\u8BF7\u5148\u653E\u5165\u5531\u7247\u6216\u64AD\u653E\u5217\u8868!");
            return;
        }

        boolean isPlaylist = yincmewy.netmusiccanneedqq.compat.PlaylistCompat.isPlaylistItem(slotItem);

        if (isPlaylist) {
            waitingAddCount++;
            int lastCount = yincmewy.netmusiccanneedqq.compat.PlaylistCompat.getSongCount(slotItem);
            NetMusicCanNeedQQNetwork.CHANNEL.sendToServer(
                    new PhoneAddToPlaylistMessage(List.of(new PhoneAddToPlaylistMessage.SongEntry(
                            mid, song.songName, song.songTime))));
            statusText = Component.literal("\u00A7e\u6B63\u5728\u6DFB\u52A0: " + song.songName + "...");
        } else {
            var existingInfo = com.github.tartaricacid.netmusic.item.ItemMusicCD.getSongInfo(slotItem);
            lastSlotSongUrl = existingInfo != null && existingInfo.songUrl != null ? existingInfo.songUrl : "";
            waitingBurn = true;
            NetMusicCanNeedQQNetwork.CHANNEL.sendToServer(
                    new PhoneBurnMessage(mid, ClientConfig.getQuality()));
            statusText = Component.literal("\u00A7e\u6B63\u5728\u523B\u5F55: " + song.songName + "...");
        }
    }

    private void startQrLogin() {
        loggingIn = true;
        statusText = Component.literal("\u6B63\u5728\u83B7\u53D6\u4E8C\u7EF4\u7801...");

        QqLoginService.fetchQrCode().whenComplete((imageData, err) -> {
            Minecraft.getInstance().execute(() -> {
                if (err != null || imageData == null) {
                    statusText = Component.literal("\u83B7\u53D6\u4E8C\u7EF4\u7801\u5931\u8D25");
                    loggingIn = false;
                    return;
                }
                if (qrRenderer.load(imageData)) {
                    statusText = Component.literal("\u8BF7\u4F7F\u7528\u624B\u673A QQ \u626B\u63CF\u4E8C\u7EF4\u7801");
                    pollTimer = 40;
                } else {
                    statusText = Component.literal("\u4E8C\u7EF4\u7801\u52A0\u8F7D\u5931\u8D25");
                    loggingIn = false;
                }
            });
        });
    }

    private void pollLoginStatus() {
        QqLoginService.pollLogin().whenComplete((state, err) -> {
            Minecraft.getInstance().execute(() -> {
                if (err != null) {
                    statusText = Component.literal("\u767B\u5F55\u8F6E\u8BE2\u5931\u8D25");
                    loggingIn = false;
                    return;
                }
                switch (state) {
                    case SUCCESS -> {
                        statusText = Component.literal("\u2714 \u767B\u5F55\u6210\u529F! musicid: " + QqCredentialManager.getMusicId());
                        loggingIn = false;
                        qrRenderer.release();
                    }
                    case QR_EXPIRED -> {
                        statusText = Component.literal("\u4E8C\u7EF4\u7801\u5DF2\u8FC7\u671F\uFF0C\u8BF7\u91CD\u65B0\u83B7\u53D6");
                        loggingIn = false;
                        qrRenderer.release();
                    }
                    case FAILED -> {
                        statusText = Component.literal("\u767B\u5F55\u5931\u8D25");
                        loggingIn = false;
                        qrRenderer.release();
                    }
                    default -> pollTimer = 40;
                }
            });
        });
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void removed() {
        super.removed();
        if (qrRenderer != null) {
            qrRenderer.release();
        }
    }
}
