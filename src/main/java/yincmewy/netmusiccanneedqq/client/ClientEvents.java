package yincmewy.netmusiccanneedqq.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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

    @EventBusSubscriber(modid = Netmusiccanneedqq.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_CONFIG_KEY);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(Netmusiccanneedqq.PHONE_MENU.get(), PhoneScreen::new);
        }
    }

    @EventBusSubscriber(modid = Netmusiccanneedqq.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static final class GameBusEvents {
        private GameBusEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null && OPEN_CONFIG_KEY.consumeClick() && ModList.get().isLoaded("cloth_config")) {
                mc.setScreen(ClientConfigScreen.create(null));
            }
        }
    }
}
