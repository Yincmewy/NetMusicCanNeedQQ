package yincmewy.netmusiccanneedqq.network;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.config.QualityLevel;
import yincmewy.netmusiccanneedqq.data.SongInfoData;
import yincmewy.netmusiccanneedqq.item.PhoneMenu;
import yincmewy.netmusiccanneedqq.qq.QqDiscNbt;
import yincmewy.netmusiccanneedqq.qq.QqMusicUpdater;
import yincmewy.netmusiccanneedqq.qq.QqMusicUtils;

public record PhoneBurnMessage(String songMid, QualityLevel quality) implements CustomPacketPayload {
    public static final Type<PhoneBurnMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Netmusiccanneedqq.MODID, "phone_burn"));
    public static final StreamCodec<ByteBuf, PhoneBurnMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    PhoneBurnMessage::songMid,
                    ByteBufCodecs.idMapper(ordinal -> QualityLevel.values()[ordinal], QualityLevel::ordinal),
                    PhoneBurnMessage::quality,
                    PhoneBurnMessage::new);

    public PhoneBurnMessage {
        songMid = songMid == null ? "" : songMid;
        quality = quality == null ? QualityLevel.HIGH : quality;
    }

    public static void handle(PhoneBurnMessage message, IPayloadContext context) {
        if (!context.flow().isServerbound()) {
            return;
        }
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof PhoneMenu menu)) {
                return;
            }

            ItemStack cd = menu.getInputStack();
            if (cd.isEmpty() || !(cd.getItem() instanceof ItemMusicCD)) {
                return;
            }

            Util.backgroundExecutor().execute(() -> {
                try {
                    SongInfoData songInfo = QqMusicUtils.resolveSong(message.songMid(), null, message.quality());
                    if (songInfo == null || !songInfo.isValid()) {
                        return;
                    }

                    QqMusicUpdater.prefetch(message.songMid(), message.quality());

                    player.server.execute(() -> {
                        ItemStack current = menu.getInputStack();
                        if (current.isEmpty()) {
                            return;
                        }
                        QqDiscNbt.markQq(current, message.songMid(), message.quality());

                        ItemMusicCD.SongInfo info = new ItemMusicCD.SongInfo(
                                songInfo.songUrl, songInfo.songName, songInfo.songTime, false);
                        ItemMusicCD.setSongInfo(info, current);
                        menu.getInputContainer().setChanged();
                    });
                } catch (Exception e) {
                    Netmusiccanneedqq.LOGGER.error("Phone burn failed", e);
                }
            });
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
