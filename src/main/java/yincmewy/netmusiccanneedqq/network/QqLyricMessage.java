package yincmewy.netmusiccanneedqq.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.qq.QqLyricClientHandler;

/**
 * 从服务器发送到客户端的QQ音乐歌词消息
 * 包含songmid和方块位置，客户端收到后从QQ音乐API获取歌词
 */
public record QqLyricMessage(BlockPos pos, String songmid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<QqLyricMessage> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Netmusiccanneedqq.MODID, "qq_lyric"));
    public static final StreamCodec<ByteBuf, QqLyricMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, QqLyricMessage::pos,
            ByteBufCodecs.STRING_UTF8, QqLyricMessage::songmid,
            QqLyricMessage::new);

    public static void handle(QqLyricMessage message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> onHandle(message));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void onHandle(QqLyricMessage message) {
        QqLyricClientHandler.storeSongmid(message.pos(), message.songmid());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
