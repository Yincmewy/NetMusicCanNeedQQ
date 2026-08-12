package yincmewy.netmusiccanneedqq.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import yincmewy.netmusiccanneedqq.qq.QqLyricClientHandler;

import java.util.function.Supplier;

/**
 * 从服务器发送到客户端的QQ音乐歌词消息
 * 包含songmid和方块位置，客户端收到后从QQ音乐API获取歌词
 */
public class QqLyricMessage {
    private final BlockPos pos;
    private final String songmid;

    public QqLyricMessage(BlockPos pos, String songmid) {
        this.pos = pos;
        this.songmid = songmid;
    }

    public static QqLyricMessage decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String songmid = buf.readUtf(32767);
        return new QqLyricMessage(pos, songmid);
    }

    public static void encode(QqLyricMessage message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.pos);
        buf.writeUtf(message.songmid == null ? "" : message.songmid);
    }

    public static void handle(QqLyricMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> onHandle(message));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void onHandle(QqLyricMessage message) {
        QqLyricClientHandler.storeSongmid(message.pos, message.songmid);
    }
}
