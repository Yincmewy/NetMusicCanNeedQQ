package yincmewy.netmusiccanneedqq.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;

@Mod.EventBusSubscriber(modid = Netmusiccanneedqq.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.EnumValue<ProviderType> DEFAULT_PROVIDER = BUILDER
            .comment("Default provider for the NetMusic CD burner")
            .defineEnum("defaultProvider", ProviderType.NETEASE);
    public static final ForgeConfigSpec.ConfigValue<String> VIP_COOKIE = BUILDER
            .comment("VIP cookie for QQ Music")
            .define("vipCookie", "");

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static ProviderType currentProvider = ProviderType.NETEASE;
    private static String currentVipCookie = "";

    private ClientConfig() {
    }

    public static ProviderType getProvider() {
        return currentProvider;
    }

    public static void setProvider(ProviderType provider) {
        currentProvider = provider;
        DEFAULT_PROVIDER.set(provider);
    }

    public static String getVipCookie() {
        return currentVipCookie;
    }

    public static boolean hasVipCookie() {
        return currentVipCookie != null && !currentVipCookie.isBlank();
    }

    public static void setVipCookie(String cookie) {
        currentVipCookie = sanitizeCookie(cookie);
        VIP_COOKIE.set(currentVipCookie);
    }

    private static String sanitizeCookie(String cookie) {
        if (cookie == null) {
            return "";
        }
        return cookie.trim();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            currentProvider = DEFAULT_PROVIDER.get();
            currentVipCookie = sanitizeCookie(VIP_COOKIE.get());
        }
    }
}
