package yincmewy.netmusiccanneedqq.config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.network.ServerVipCookieStateSyncHandler;

@EventBusSubscriber(modid = Netmusiccanneedqq.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> VIP_COOKIE = BUILDER
            .comment("VIP cookie for QQ Music, only used on server side")
            .define("vipCookie", "");

    public static final ModConfigSpec SPEC = BUILDER.build();

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
