package yincmewy.netmusiccanneedqq.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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
        category.addEntry(entryBuilder.startStrField(Component.literal("VIP Cookie"), ClientConfig.getVipCookie())
                .setDefaultValue("")
                .setSaveConsumer(ClientConfig::setVipCookie)
                .build());

        return builder.build();
    }
}
