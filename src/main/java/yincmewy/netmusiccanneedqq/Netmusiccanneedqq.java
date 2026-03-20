package yincmewy.netmusiccanneedqq;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
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

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    public static final RegistryObject<Item> PHONE = ITEMS.register("phone", PhoneItem::new);
    public static final RegistryObject<MenuType<PhoneMenu>> PHONE_MENU = MENUS.register("phone",
            () -> new MenuType<>(PhoneMenu::new, FeatureFlags.DEFAULT_FLAGS));

    @SuppressWarnings("removal")
    public Netmusiccanneedqq() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(Netmusiccanneedqq::addCreativeTabItems);

        NetMusicCanNeedQQNetwork.init();
        QqCredentialManager.init(FMLPaths.CONFIGDIR.get());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ServerConfig.SPEC);
        if (FMLEnvironment.dist.isClient() && ModList.get().isLoaded("cloth_config")) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> ClientConfigScreen.create(parent)));
        }
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PHONE);
        }
    }
}
