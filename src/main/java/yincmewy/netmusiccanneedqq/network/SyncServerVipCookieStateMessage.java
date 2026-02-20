package yincmewy.netmusiccanneedqq.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.config.VipCookieState;

public record SyncServerVipCookieStateMessage(boolean hasServerVipCookie) implements CustomPacketPayload {
    public static final Type<SyncServerVipCookieStateMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Netmusiccanneedqq.MODID, "sync_server_vip_cookie_state"));
    public static final StreamCodec<ByteBuf, SyncServerVipCookieStateMessage> STREAM_CODEC =
            StreamCodec.of((buffer, message) -> buffer.writeBoolean(message.hasServerVipCookie()),
                    buffer -> new SyncServerVipCookieStateMessage(buffer.readBoolean()));

    public static void handle(SyncServerVipCookieStateMessage message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            return;
        }
        context.enqueueWork(() -> VipCookieState.setServerVipCookieAvailable(message.hasServerVipCookie()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
