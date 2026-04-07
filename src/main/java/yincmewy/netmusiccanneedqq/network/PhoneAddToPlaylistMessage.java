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
import yincmewy.netmusiccanneedqq.compat.PlaylistCompat;
import yincmewy.netmusiccanneedqq.config.QualityLevel;
import yincmewy.netmusiccanneedqq.data.SongInfoData;
import yincmewy.netmusiccanneedqq.item.PhoneMenu;
import yincmewy.netmusiccanneedqq.qq.QqMusicUtils;

import java.util.ArrayList;
import java.util.List;

public record PhoneAddToPlaylistMessage(List<SongEntry> entries) implements CustomPacketPayload {
    public static final Type<PhoneAddToPlaylistMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Netmusiccanneedqq.MODID, "phone_add_to_playlist"));
    public static final StreamCodec<ByteBuf, PhoneAddToPlaylistMessage> STREAM_CODEC =
            StreamCodec.of(
                    PhoneAddToPlaylistMessage::encode,
                    PhoneAddToPlaylistMessage::decode);

    public PhoneAddToPlaylistMessage {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    private static PhoneAddToPlaylistMessage decode(ByteBuf buffer) {
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        List<SongEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(SongEntry.STREAM_CODEC.decode(buffer));
        }
        return new PhoneAddToPlaylistMessage(entries);
    }

    private static void encode(ByteBuf buffer, PhoneAddToPlaylistMessage message) {
        ByteBufCodecs.VAR_INT.encode(buffer, message.entries().size());
        for (SongEntry entry : message.entries()) {
            SongEntry.STREAM_CODEC.encode(buffer, entry);
        }
    }

    public static void handle(PhoneAddToPlaylistMessage message, IPayloadContext context) {
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
            if (!PlaylistCompat.isAvailable()) {
                return;
            }

            ItemStack playlist = menu.getInputStack();
            if (playlist.isEmpty() || !PlaylistCompat.isPlaylistItem(playlist)) {
                return;
            }

            Util.backgroundExecutor().execute(() -> {
                for (SongEntry entry : message.entries()) {
                    try {
                        SongInfoData songInfo = QqMusicUtils.resolveSong(entry.mid(), null, QualityLevel.HIGH);
                        if (songInfo == null || !songInfo.isValid()) {
                            continue;
                        }

                        player.server.execute(() -> {
                            ItemStack current = menu.getInputStack();
                            if (current.isEmpty()) {
                                return;
                            }
                            ItemMusicCD.SongInfo info = new ItemMusicCD.SongInfo(
                                    songInfo.songUrl, songInfo.songName, songInfo.songTime, false);
                            PlaylistCompat.addSong(current, info);
                            menu.getInputContainer().setChanged();
                        });
                    } catch (Exception e) {
                        Netmusiccanneedqq.LOGGER.error("Failed to add song to playlist: {}", entry.mid(), e);
                    }
                }
            });
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record SongEntry(String mid, String name, int duration) {
        private static final StreamCodec<ByteBuf, SongEntry> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8,
                        SongEntry::mid,
                        ByteBufCodecs.STRING_UTF8,
                        SongEntry::name,
                        ByteBufCodecs.VAR_INT,
                        SongEntry::duration,
                        SongEntry::new);

        public SongEntry {
            mid = mid == null ? "" : mid;
            name = name == null ? "" : name;
        }
    }
}
