package yincmewy.netmusiccanneedqq.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.config.ServerConfig;

@Mod.EventBusSubscriber(modid = Netmusiccanneedqq.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerVipCookieStateSyncHandler {
    private ServerVipCookieStateSyncHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncTo(player);
        }
    }

    public static void syncToAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncTo(player);
        }
    }

    private static void syncTo(ServerPlayer player) {
        if (player == null) {
            return;
        }
        boolean hasServerVipCookie = ServerConfig.hasVipCookie();
        NetMusicCanNeedQQNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncServerVipCookieStateMessage(hasServerVipCookie));
    }
}
