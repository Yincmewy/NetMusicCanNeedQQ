package yincmewy.netmusiccanneedqq;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import yincmewy.netmusiccanneedqq.config.ClientConfig;
import yincmewy.netmusiccanneedqq.config.ClientConfigScreen;
import yincmewy.netmusiccanneedqq.network.NetMusicCanNeedQQNetwork;

@Mod(Netmusiccanneedqq.MODID)
public class Netmusiccanneedqq {

    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final String MODID = "netmusiccanneedqq";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public Netmusiccanneedqq() {
        NetMusicCanNeedQQNetwork.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        if (FMLEnvironment.dist.isClient() && ModList.get().isLoaded("cloth_config")) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> ClientConfigScreen.create(parent)));
        }
    }
}
