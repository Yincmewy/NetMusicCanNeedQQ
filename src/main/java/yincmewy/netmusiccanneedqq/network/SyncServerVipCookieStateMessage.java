package yincmewy.netmusiccanneedqq.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import yincmewy.netmusiccanneedqq.config.VipCookieState;

import java.util.function.Supplier;

public class SyncServerVipCookieStateMessage {
    private final boolean hasServerVipCookie;

    public SyncServerVipCookieStateMessage(boolean hasServerVipCookie) {
        this.hasServerVipCookie = hasServerVipCookie;
    }

    public static SyncServerVipCookieStateMessage decode(FriendlyByteBuf buf) {
        return new SyncServerVipCookieStateMessage(buf.readBoolean());
    }

    public static void encode(SyncServerVipCookieStateMessage message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.hasServerVipCookie);
    }

    public static void handle(SyncServerVipCookieStateMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> VipCookieState.setServerVipCookieAvailable(message.hasServerVipCookie));
        }
        context.setPacketHandled(true);
    }
}
