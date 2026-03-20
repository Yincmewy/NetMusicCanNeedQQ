package yincmewy.netmusiccanneedqq.network;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.compat.PlaylistCompat;
import yincmewy.netmusiccanneedqq.config.QualityLevel;
import yincmewy.netmusiccanneedqq.data.SongInfoData;
import yincmewy.netmusiccanneedqq.item.PhoneMenu;
import yincmewy.netmusiccanneedqq.qq.QqMusicUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class PhoneAddToPlaylistMessage {
    private final List<SongEntry> entries;

    public PhoneAddToPlaylistMessage(List<SongEntry> entries) {
        this.entries = entries;
    }

    public static PhoneAddToPlaylistMessage decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<SongEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new SongEntry(buf.readUtf(32767), buf.readUtf(32767), buf.readVarInt()));
        }
        return new PhoneAddToPlaylistMessage(entries);
    }

    public static void encode(PhoneAddToPlaylistMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entries.size());
        for (SongEntry e : msg.entries) {
            buf.writeUtf(e.mid == null ? "" : e.mid);
            buf.writeUtf(e.name == null ? "" : e.name);
            buf.writeVarInt(e.duration);
        }
    }

    public static void handle(PhoneAddToPlaylistMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        if (ctx.getDirection().getReceptionSide().isServer()) {
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;
                if (!(player.containerMenu instanceof PhoneMenu menu)) return;
                if (!PlaylistCompat.isAvailable()) return;

                ItemStack playlist = menu.getInputStack();
                if (playlist.isEmpty() || !PlaylistCompat.isPlaylistItem(playlist)) return;

                Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "netmusiccanneedqq-phone-playlist");
                    t.setDaemon(true);
                    return t;
                }).execute(() -> {
                    for (SongEntry entry : msg.entries) {
                        try {
                            SongInfoData songInfo = QqMusicUtils.resolveSong(entry.mid, null, QualityLevel.HIGH);
                            if (songInfo == null || !songInfo.isValid()) continue;

                            player.server.execute(() -> {
                                ItemStack current = menu.getInputStack();
                                if (current.isEmpty()) return;
                                ItemMusicCD.SongInfo info = new ItemMusicCD.SongInfo(
                                        songInfo.songUrl, songInfo.songName, songInfo.songTime, false);
                                PlaylistCompat.addSong(current, info);
                                menu.getInputContainer().setChanged();
                            });
                        } catch (Exception e) {
                            Netmusiccanneedqq.LOGGER.error("Failed to add song to playlist: {}", entry.mid, e);
                        }
                    }
                });
            });
        }
        ctx.setPacketHandled(true);
    }

    public static class SongEntry {
        public final String mid;
        public final String name;
        public final int duration;

        public SongEntry(String mid, String name, int duration) {
            this.mid = mid;
            this.name = name;
            this.duration = duration;
        }
    }
}
