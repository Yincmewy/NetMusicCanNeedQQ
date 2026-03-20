package yincmewy.netmusiccanneedqq.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;

public final class NetMusicCanNeedQQNetwork {
    private static final String PROTOCOL = "5";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Netmusiccanneedqq.MODID, "network"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private NetMusicCanNeedQQNetwork() {
    }

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, MarkQqDiscMessage.class, MarkQqDiscMessage::encode, MarkQqDiscMessage::decode, MarkQqDiscMessage::handle);
        CHANNEL.registerMessage(id++, ClearQqDiscMessage.class, ClearQqDiscMessage::encode, ClearQqDiscMessage::decode, ClearQqDiscMessage::handle);
        CHANNEL.registerMessage(id++, SyncServerVipCookieStateMessage.class, SyncServerVipCookieStateMessage::encode, SyncServerVipCookieStateMessage::decode, SyncServerVipCookieStateMessage::handle);
        CHANNEL.registerMessage(id++, PhoneBurnMessage.class, PhoneBurnMessage::encode, PhoneBurnMessage::decode, PhoneBurnMessage::handle);
        CHANNEL.registerMessage(id++, PhoneAddToPlaylistMessage.class, PhoneAddToPlaylistMessage::encode, PhoneAddToPlaylistMessage::decode, PhoneAddToPlaylistMessage::handle);
    }
}
