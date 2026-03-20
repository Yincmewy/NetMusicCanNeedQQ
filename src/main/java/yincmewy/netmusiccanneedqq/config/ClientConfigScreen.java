package yincmewy.netmusiccanneedqq.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
                ? "\u2714 \u5DF2\u767B\u5F55 (musicid: " + QqCredentialManager.getMusicId() + ")"
                : "\u2716 \u672A\u767B\u5F55 (\u8BF7\u4F7F\u7528\u624B\u673A\u7269\u54C1\u626B\u7801\u767B\u5F55)";
        category.addEntry(entryBuilder.startTextDescription(Component.literal(loginStatus))
                .build());

        category.addEntry(entryBuilder.startStrField(Component.literal("VIP Cookie (\u5907\u7528)"), ClientConfig.getVipCookie())
                .setDefaultValue("")
                .setSaveConsumer(ClientConfig::setVipCookie)
                .setTooltip(Component.literal("\u4EC5\u5728\u672A\u626B\u7801\u767B\u5F55\u65F6\u4F7F\u7528"))
                .build());

        category.addEntry(entryBuilder.startEnumSelector(Component.literal("\u97F3\u8D28"), QualityLevel.class, ClientConfig.getQuality())
                .setDefaultValue(QualityLevel.HIGH)
                .setEnumNameProvider(value -> Component.literal(((QualityLevel) value).getLabel()))
                .setSaveConsumer(ClientConfig::setQuality)
                .build());

        return builder.build();
    }
}
