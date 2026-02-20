package yincmewy.netmusiccanneedqq.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetMusicCanNeedQQNetwork {
    private static final String PROTOCOL = "4";

    private NetMusicCanNeedQQNetwork() {
    }

    public static void registerPacket(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL).optional();
        registrar.playToServer(MarkQqDiscMessage.TYPE, MarkQqDiscMessage.STREAM_CODEC, MarkQqDiscMessage::handle);
        registrar.playToServer(ClearQqDiscMessage.TYPE, ClearQqDiscMessage.STREAM_CODEC, ClearQqDiscMessage::handle);
        registrar.playToClient(SyncServerVipCookieStateMessage.TYPE, SyncServerVipCookieStateMessage.STREAM_CODEC, SyncServerVipCookieStateMessage::handle);
    }

    public static void sendToServer(CustomPacketPayload message) {
        PacketDistributor.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload message) {
        PacketDistributor.sendToPlayer(player, message);
    }
}
