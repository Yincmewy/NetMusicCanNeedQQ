package yincmewy.netmusiccanneedqq.qq;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.data.SongInfoData;

public final class QqMusicUpdater {
    private QqMusicUpdater() {
    }

    public static ItemMusicCD.SongInfo refreshIfNeeded(ItemStack stack, ItemMusicCD.SongInfo info) {
        if (info == null || !QqDiscNbt.isQqDisc(stack) || !isServerThread()) {
            return info;
        }
        String qqInput = QqDiscNbt.getQqInput(stack);
        if (qqInput == null || qqInput.isBlank()) {
            return info;
        }
        try {
            SongInfoData updated = QqMusicUtils.resolveSong(qqInput);
            if (updated == null || updated.songUrl == null || updated.songUrl.isBlank()) {
                return info;
            }
            info.songUrl = updated.songUrl;
            if (updated.songTime > 0) {
                info.songTime = updated.songTime;
            }
            if (updated.songName != null && !updated.songName.isBlank()) {
                info.songName = updated.songName;
            }
            ItemMusicCD.setSongInfo(info, stack);
        } catch (Exception e) {
            Netmusiccanneedqq.LOGGER.error("Failed to refresh QQ music url", e);
        }
        return info;
    }

    private static boolean isServerThread() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSameThread();
    }
}
