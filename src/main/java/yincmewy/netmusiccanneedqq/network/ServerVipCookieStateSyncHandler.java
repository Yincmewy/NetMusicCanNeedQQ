package yincmewy.netmusiccanneedqq.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.config.ServerConfig;

@EventBusSubscriber(modid = Netmusiccanneedqq.MODID, bus = EventBusSubscriber.Bus.GAME)
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
        NetMusicCanNeedQQNetwork.sendToPlayer(player, new SyncServerVipCookieStateMessage(hasServerVipCookie));
    }
}
