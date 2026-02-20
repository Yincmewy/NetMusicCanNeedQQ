package yincmewy.netmusiccanneedqq.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.network.ServerVipCookieStateSyncHandler;

@Mod.EventBusSubscriber(modid = Netmusiccanneedqq.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<String> VIP_COOKIE = BUILDER
            .comment("VIP cookie for QQ Music, only used on server side")
            .define("vipCookie", "");

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static String currentVipCookie = "";

    private ServerConfig() {
    }

    public static String getVipCookie() {
        return currentVipCookie;
    }

    public static boolean hasVipCookie() {
        return currentVipCookie != null && !currentVipCookie.isBlank();
    }

    private static String sanitizeCookie(String cookie) {
        if (cookie == null) {
            return "";
        }
        return cookie.trim();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        currentVipCookie = sanitizeCookie(VIP_COOKIE.get());
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> ServerVipCookieStateSyncHandler.syncToAll(server));
        }
    }
}
