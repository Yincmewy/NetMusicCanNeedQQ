package yincmewy.netmusiccanneedqq.network;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.compat.NetMusicCompat;
import yincmewy.netmusiccanneedqq.config.QualityLevel;
import yincmewy.netmusiccanneedqq.data.SongInfoData;
import yincmewy.netmusiccanneedqq.item.PhoneMenu;
import yincmewy.netmusiccanneedqq.qq.QqDiscNbt;
import yincmewy.netmusiccanneedqq.qq.QqMusicUtils;

import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class PhoneBurnMessage {
    private final String songMid;
    private final QualityLevel quality;

    public PhoneBurnMessage(String songMid, QualityLevel quality) {
        this.songMid = songMid;
        this.quality = quality != null ? quality : QualityLevel.HIGH;
    }

    public static PhoneBurnMessage decode(FriendlyByteBuf buf) {
        String mid = buf.readUtf(32767);
        QualityLevel q;
        try {
            q = buf.readEnum(QualityLevel.class);
        } catch (Exception e) {
            q = QualityLevel.HIGH;
        }
        return new PhoneBurnMessage(mid, q);
    }

    public static void encode(PhoneBurnMessage msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.songMid == null ? "" : msg.songMid);
        buf.writeEnum(msg.quality);
    }

    public static void handle(PhoneBurnMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        if (ctx.getDirection().getReceptionSide().isServer()) {
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;
                if (!(player.containerMenu instanceof PhoneMenu menu)) return;

                ItemStack cd = menu.getInputStack();
                if (cd.isEmpty() || !(cd.getItem() instanceof ItemMusicCD)) return;

                Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "netmusiccanneedqq-phone-burn");
                    t.setDaemon(true);
                    return t;
                }).execute(() -> {
                    try {
                        SongInfoData songInfo = QqMusicUtils.resolveSong(msg.songMid, null, msg.quality);
                        if (songInfo == null || !songInfo.isValid()) return;

                        player.server.execute(() -> {
                            ItemStack current = menu.getInputStack();
                            if (current.isEmpty()) return;
                            QqDiscNbt.markQq(current, msg.songMid, msg.quality);

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
        ctx.setPacketHandled(true);
    }
}
