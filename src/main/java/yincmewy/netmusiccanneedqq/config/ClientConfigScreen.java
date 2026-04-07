package yincmewy.netmusiccanneedqq.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import yincmewy.netmusiccanneedqq.qq.QqCredentialManager;

@OnlyIn(Dist.CLIENT)
public final class ClientConfigScreen {
    private ClientConfigScreen() {
    }

    public static Screen create(Screen parent) {
        var builder = ConfigBuilder.create();
        builder.setParentScreen(parent);
        builder.setTitle(Component.literal("NetMusicCanNeedQQ"));
        var entryBuilder = builder.entryBuilder();

        var category = builder.getOrCreateCategory(Component.literal("QQ"));

        String loginStatus = QqCredentialManager.hasValidCredential()
                ? "已登录 (musicid: " + QqCredentialManager.getMusicId() + ")"
                : "未登录 (请使用手机物品扫码登录)";
        category.addEntry(entryBuilder.startTextDescription(Component.literal(loginStatus)).build());

        category.addEntry(entryBuilder.startStrField(Component.literal("VIP Cookie (备用)"), ClientConfig.getVipCookie())
                .setDefaultValue("")
                .setSaveConsumer(ClientConfig::setVipCookie)
                .setTooltip(Component.literal("仅在未扫码登录时使用"))
                .build());

        category.addEntry(entryBuilder.startEnumSelector(Component.literal("音质"), QualityLevel.class, ClientConfig.getQuality())
                .setDefaultValue(QualityLevel.HIGH)
                .setEnumNameProvider(value -> Component.literal(((QualityLevel) value).getLabel()))
                .setSaveConsumer(ClientConfig::setQuality)
                .build());

        return builder.build();
    }
}
