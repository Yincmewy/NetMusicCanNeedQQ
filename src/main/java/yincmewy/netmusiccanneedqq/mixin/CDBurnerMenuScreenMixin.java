package yincmewy.netmusiccanneedqq.mixin;

import com.github.tartaricacid.netmusic.client.gui.CDBurnerMenuScreen;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yincmewy.netmusiccanneedqq.client.QqSearchScreen;
import yincmewy.netmusiccanneedqq.client.QqSearchState;
import yincmewy.netmusiccanneedqq.client.QqSearchTarget;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.compat.NetMusicCompat;
import yincmewy.netmusiccanneedqq.config.ClientConfig;
import yincmewy.netmusiccanneedqq.config.ProviderType;
import yincmewy.netmusiccanneedqq.data.SongInfoData;
import yincmewy.netmusiccanneedqq.network.ClearQqDiscMessage;
import yincmewy.netmusiccanneedqq.network.MarkQqDiscMessage;
import yincmewy.netmusiccanneedqq.network.NetMusicCanNeedQQNetwork;
import yincmewy.netmusiccanneedqq.qq.QqDiscNbt;
import yincmewy.netmusiccanneedqq.qq.QqMusicUtils;

@Mixin(value = CDBurnerMenuScreen.class, remap = false)
public abstract class CDBurnerMenuScreenMixin extends AbstractContainerScreen<AbstractContainerMenu> implements QqSearchTarget {
    @Unique
    private static final int NETMUSICCANNEEDQQ_SEARCH_BUTTON_WIDTH = 55;
    @Unique
    private static final int NETMUSICCANNEEDQQ_SEARCH_BUTTON_HEIGHT = 18;
    @Unique
    private static final int NETMUSICCANNEEDQQ_SEARCH_GAP = 4;

    @Shadow
    private EditBox textField;
    @Shadow
    private Checkbox readOnlyButton;
    @Shadow
    private Component tips;

    @Unique
    private Button netmusiccanneedqq$providerButton;
    @Unique
    private Button netmusiccanneedqq$searchButton;
    @Unique
    private int netmusiccanneedqq$baseTextFieldWidth = -1;

    protected CDBurnerMenuScreenMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0, remap = true)
    private void netmusiccanneedqq$init(CallbackInfo ci) {
        netmusiccanneedqq$initCommon();
    }

    @Inject(method = "resize", at = @At("TAIL"), require = 0, remap = true)
    private void netmusiccanneedqq$resize(Minecraft minecraft, int width, int height, CallbackInfo ci) {
        netmusiccanneedqq$updateSearchUi();
    }

    @Inject(method = "handleCraftButton", at = @At("HEAD"), cancellable = true)
    private void netmusiccanneedqq$handleCraftButton(CallbackInfo ci) {
        if (ClientConfig.getProvider() != ProviderType.QQ) {
            netmusiccanneedqq$clearQqDiscTag();
            return;
        }
        Slot inputSlot = this.getMenu().getSlot(0);
        ItemStack cd = inputSlot.getItem();
        if (cd.isEmpty()) {
            this.tips = Component.translatable("gui.netmusic.cd_burner.cd_is_empty");
            ci.cancel();
            return;
        }
        if (netmusiccanneedqq$isReadOnly(cd)) {
            this.tips = Component.translatable("gui.netmusic.cd_burner.cd_read_only");
            ci.cancel();
            return;
        }
        if (Util.isBlank(textField.getValue())) {
            this.tips = Component.translatable("gui.netmusic.cd_burner.no_music_id");
            ci.cancel();
            return;
        }
        try {
            SongInfoData songInfo = QqMusicUtils.resolveSong(textField.getValue());
            if (songInfo == null || !songInfo.isValid()) {
                this.tips = Component.translatable("gui.netmusic.cd_burner.get_info_error");
                ci.cancel();
                return;
            }
            songInfo.readOnly = this.readOnlyButton.selected();
            if (songInfo.vip && !ClientConfig.hasVipCookie()) {
                netmusiccanneedqq$showVipCookieToast();
            }
            songInfo.vip = false;
            NetMusicCanNeedQQNetwork.CHANNEL.sendToServer(new MarkQqDiscMessage(textField.getValue()));
            if (!NetMusicCompat.sendSongToServer(songInfo)) {
                this.tips = Component.translatable("gui.netmusic.cd_burner.get_info_error");
            }
        } catch (Exception e) {
            this.tips = Component.translatable("gui.netmusic.cd_burner.get_info_error");
            Netmusiccanneedqq.LOGGER.error("Failed to resolve QQ music info", e);
        }
        ci.cancel();
    }

    @Override
    public void netmusiccanneedqq$applySearchResult(String value) {
        if (this.textField != null) {
            this.textField.setValue(value);
        }
    }

    @Unique
    private Component netmusiccanneedqq$getProviderLabel() {
        return Component.literal(ClientConfig.getProvider().getShortLabel());
    }

    @Unique
    private void netmusiccanneedqq$toggleProvider() {
        ClientConfig.setProvider(ClientConfig.getProvider().next());
        if (this.netmusiccanneedqq$providerButton != null) {
            this.netmusiccanneedqq$providerButton.setMessage(netmusiccanneedqq$getProviderLabel());
        }
        netmusiccanneedqq$updateSearchUi();
    }

    @Unique
    private void netmusiccanneedqq$openSearch() {
        if (ClientConfig.getProvider() != ProviderType.QQ || this.textField == null) {
            return;
        }
        QqSearchState.setOpening(true);
        try {
            Minecraft.getInstance().setScreen(new QqSearchScreen((CDBurnerMenuScreen) (Object) this, this.textField.getValue()));
        } finally {
            QqSearchState.setOpening(false);
        }
    }

    @Unique
    private void netmusiccanneedqq$updateSearchUi() {
        if (this.textField == null) {
            return;
        }
        if (this.netmusiccanneedqq$baseTextFieldWidth < 0) {
            this.netmusiccanneedqq$baseTextFieldWidth = this.textField.getWidth();
        }
        boolean showSearch = ClientConfig.getProvider() == ProviderType.QQ;
        this.textField.setWidth(this.netmusiccanneedqq$baseTextFieldWidth);
        if (this.netmusiccanneedqq$searchButton != null) {
            this.netmusiccanneedqq$searchButton.visible = showSearch;
            this.netmusiccanneedqq$searchButton.active = showSearch;
            int searchX = this.netmusiccanneedqq$providerButton != null
                    ? this.netmusiccanneedqq$providerButton.getX() + this.netmusiccanneedqq$providerButton.getWidth() + NETMUSICCANNEEDQQ_SEARCH_GAP
                    : this.textField.getX();
            int searchY = this.netmusiccanneedqq$providerButton != null
                    ? this.netmusiccanneedqq$providerButton.getY()
                    : this.textField.getY();
            this.netmusiccanneedqq$searchButton.setX(searchX);
            this.netmusiccanneedqq$searchButton.setY(searchY);
        }
    }

    @Unique
    private void netmusiccanneedqq$initCommon() {
        int x = this.leftPos + 7;
        int y = this.topPos + 71;
        this.netmusiccanneedqq$providerButton = Button.builder(netmusiccanneedqq$getProviderLabel(), button -> netmusiccanneedqq$toggleProvider())
                .pos(x, y)
                .size(55, 18)
                .build();
        this.addRenderableWidget(this.netmusiccanneedqq$providerButton);

        this.netmusiccanneedqq$searchButton = Button.builder(Component.literal("\u641C\u7D22\u6B4C\u66F2"), button -> netmusiccanneedqq$openSearch())
                .pos(this.leftPos + 12, this.topPos + 18)
                .size(NETMUSICCANNEEDQQ_SEARCH_BUTTON_WIDTH, NETMUSICCANNEEDQQ_SEARCH_BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(this.netmusiccanneedqq$searchButton);
        netmusiccanneedqq$updateSearchUi();
    }

    @Unique
    private void netmusiccanneedqq$clearQqDiscTag() {
        Slot inputSlot = this.getMenu().getSlot(0);
        ItemStack cd = inputSlot.getItem();
        if (cd.isEmpty() || !QqDiscNbt.isQqDisc(cd)) {
            return;
        }
        NetMusicCanNeedQQNetwork.CHANNEL.sendToServer(new ClearQqDiscMessage());
    }

    @Unique
    private boolean netmusiccanneedqq$isReadOnly(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("NetMusicSongInfo", Tag.TAG_COMPOUND)) {
            return false;
        }
        CompoundTag infoTag = tag.getCompound("NetMusicSongInfo");
        return infoTag.getBoolean("read_only");
    }

    @Unique
    private void netmusiccanneedqq$showVipCookieToast() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        minecraft.getToasts().addToast(new SystemToast(SystemToast.SystemToastIds.NARRATOR_TOGGLE,
                Component.literal("\u60A8\u9009\u4E2D\u7684\u662FVIP\u6B4C\u66F2\u4F46\u60A8\u6CA1\u6709\u8BBE\u7F6Evipcookie\uFF0C\u4EC5\u8BD5\u542C"),
                null));
    }
}
