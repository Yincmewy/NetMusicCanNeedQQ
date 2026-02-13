package yincmewy.netmusiccanneedqq.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetMusicCanNeedQQNetwork {
    private static final String PROTOCOL = "1.0.0";

    private NetMusicCanNeedQQNetwork() {
    }

    public static void registerPacket(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL).optional();
        registrar.playToServer(MarkQqDiscMessage.TYPE, MarkQqDiscMessage.STREAM_CODEC, MarkQqDiscMessage::handle);
        registrar.playToServer(ClearQqDiscMessage.TYPE, ClearQqDiscMessage.STREAM_CODEC, ClearQqDiscMessage::handle);
    }

    public static void sendToServer(CustomPacketPayload message) {
        PacketDistributor.sendToServer(message);
    }
}
