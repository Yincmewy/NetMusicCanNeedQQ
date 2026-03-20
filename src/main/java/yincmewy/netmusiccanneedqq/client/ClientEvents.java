package yincmewy.netmusiccanneedqq.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.config.ClientConfigScreen;

public final class ClientEvents {
    public static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.netmusiccanneedqq.open_config",
            InputConstants.UNKNOWN.getValue(),
            "key.categories.netmusiccanneedqq"
    );

    private ClientEvents() {
    }

    @Mod.EventBusSubscriber(modid = Netmusiccanneedqq.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_CONFIG_KEY);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() ->
                    MenuScreens.register(Netmusiccanneedqq.PHONE_MENU.get(), PhoneScreen::new));
        }
    }

    @Mod.EventBusSubscriber(modid = Netmusiccanneedqq.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null && OPEN_CONFIG_KEY.consumeClick()) {
                if (ModList.get().isLoaded("cloth_config")) {
                    mc.setScreen(ClientConfigScreen.create(null));
                }
            }
        }
    }
}
