package yincmewy.netmusiccanneedqq.compat;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.world.item.ItemStack;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;

import java.lang.reflect.Method;
import java.util.List;

public final class PlaylistCompat {
    private static boolean initialized;
    private static boolean available;
    private static Class<?> listItemClass;
    private static Method getSongInfoListMethod;
    private static Method setSongIndexMethod;
    private static Method setSongInfoMethod;

    private PlaylistCompat() {
    }

    private static void init() {
        if (initialized) return;
        initialized = true;
        try {
            listItemClass = Class.forName("com.gly091020.item.NetMusicListItem");
            getSongInfoListMethod = listItemClass.getMethod("getSongInfoList", ItemStack.class);
            setSongIndexMethod = listItemClass.getMethod("setSongIndex", ItemStack.class, Integer.class);
            setSongInfoMethod = listItemClass.getMethod("setSongInfo", ItemMusicCD.SongInfo.class, ItemStack.class);
            available = true;
        } catch (Exception e) {
            Netmusiccanneedqq.LOGGER.debug("net-music-play-list not available: {}", e.getMessage());
            available = false;
        }
    }

    public static boolean isAvailable() {
        init();
        return available;
    }

    public static boolean isPlaylistItem(ItemStack stack) {
        init();
        if (!available || stack.isEmpty()) return false;
        return listItemClass.isInstance(stack.getItem());
    }

    @SuppressWarnings("unchecked")
    public static int getSongCount(ItemStack stack) {
        init();
        if (!available) return 0;
        try {
            List<ItemMusicCD.SongInfo> list = (List<ItemMusicCD.SongInfo>) getSongInfoListMethod.invoke(null, stack);
            return list != null ? list.size() : 0;
        } catch (Exception e) {
            Netmusiccanneedqq.LOGGER.error("Failed to get song count", e);
            return 0;
        }
    }

    public static boolean addSong(ItemStack stack, ItemMusicCD.SongInfo songInfo) {
        init();
        if (!available) return false;
        try {
            int count = getSongCount(stack);
            setSongIndexMethod.invoke(null, stack, count);
            setSongInfoMethod.invoke(null, songInfo, stack);
            return true;
        } catch (Exception e) {
            Netmusiccanneedqq.LOGGER.error("Failed to add song to playlist", e);
            return false;
        }
    }
}
