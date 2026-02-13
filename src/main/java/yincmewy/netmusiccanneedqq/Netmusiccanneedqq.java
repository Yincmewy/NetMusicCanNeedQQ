package yincmewy.netmusiccanneedqq;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import yincmewy.netmusiccanneedqq.config.ClientConfig;
import yincmewy.netmusiccanneedqq.config.ClientConfigScreen;
import yincmewy.netmusiccanneedqq.network.NetMusicCanNeedQQNetwork;

@Mod(Netmusiccanneedqq.MODID)
public class Netmusiccanneedqq {

    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final String MODID = "netmusiccanneedqq";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Netmusiccanneedqq(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(NetMusicCanNeedQQNetwork::registerPacket);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        if (FMLEnvironment.dist.isClient() && ModList.get().isLoaded("cloth_config")) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    (container, parent) -> ClientConfigScreen.create(parent));
        }
    }
}
