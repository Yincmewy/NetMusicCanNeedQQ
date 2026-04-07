package yincmewy.netmusiccanneedqq;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import yincmewy.netmusiccanneedqq.config.ClientConfig;
import yincmewy.netmusiccanneedqq.config.ClientConfigScreen;
import yincmewy.netmusiccanneedqq.config.ServerConfig;
import yincmewy.netmusiccanneedqq.item.PhoneItem;
import yincmewy.netmusiccanneedqq.item.PhoneMenu;
import yincmewy.netmusiccanneedqq.network.NetMusicCanNeedQQNetwork;
import yincmewy.netmusiccanneedqq.qq.QqCredentialManager;

@Mod(Netmusiccanneedqq.MODID)
public class Netmusiccanneedqq {
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final String MODID = "netmusiccanneedqq";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredItem<PhoneItem> PHONE = ITEMS.register("phone", PhoneItem::new);
    public static final java.util.function.Supplier<MenuType<PhoneMenu>> PHONE_MENU = MENUS.register("phone",
            () -> new MenuType<>(PhoneMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public Netmusiccanneedqq(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        modEventBus.addListener(NetMusicCanNeedQQNetwork::registerPacket);
        modEventBus.addListener(Netmusiccanneedqq::addCreativeTabItems);

        QqCredentialManager.init(FMLPaths.CONFIGDIR.get());
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, ServerConfig.SPEC);
        if (FMLEnvironment.dist.isClient() && ModList.get().isLoaded("cloth_config")) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    new IConfigScreenFactory() {
                        @Override
                        public Screen createScreen(ModContainer container, Screen modListScreen) {
                            return ClientConfigScreen.create(modListScreen);
                        }
                    });
        }
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PHONE.get());
        }
    }
}
