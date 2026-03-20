package yincmewy.netmusiccanneedqq.qq;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.config.QualityLevel;
import yincmewy.netmusiccanneedqq.config.VipCookieState;
import yincmewy.netmusiccanneedqq.data.SongInfoData;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public final class QqMusicUpdater {
    private static final long REQUEST_COOLDOWN_MS = 28_800_000L;
    private static final ConcurrentHashMap<String, RefreshState> REFRESH_STATES = new ConcurrentHashMap<>();
    private static final ExecutorService REFRESH_EXECUTOR = Executors.newFixedThreadPool(2, new RefreshThreadFactory());

    private QqMusicUpdater() {
    }

    public static void prefetch(String qqInput, QualityLevel quality) {
        if (qqInput == null || qqInput.isBlank()) {
            return;
        }
        RefreshState state = REFRESH_STATES.computeIfAbsent(qqInput, ignored -> new RefreshState());
        state.lastRequestAt = System.currentTimeMillis();
    }

    public static ItemMusicCD.SongInfo refreshIfNeeded(ItemStack stack, ItemMusicCD.SongInfo info) {
        if (info == null || !QqDiscNbt.isQqDisc(stack)) {
            return info;
        }
        String qqInput = QqDiscNbt.getQqInput(stack);
        if (qqInput == null || qqInput.isBlank()) {
            return info;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) {
            return info;
        }
        RefreshState state = REFRESH_STATES.computeIfAbsent(qqInput, ignored -> new RefreshState());
        SongInfoData cached = state.cachedSong;
        if (cached != null) {
            applyCachedSongInfo(stack, info, cached);
        }
        long now = System.currentTimeMillis();
        if (now - state.lastRequestAt >= REQUEST_COOLDOWN_MS) {
            QualityLevel quality = QqDiscNbt.getQuality(stack);
            requestRefreshAsync(qqInput, state, now, quality);
        }
        return info;
    }

    private static void requestRefreshAsync(String qqInput, RefreshState state, long now, QualityLevel quality) {
        if (!state.inFlight.compareAndSet(false, true)) {
            return;
        }
        state.lastRequestAt = now;
        String serverVipCookie = VipCookieState.getServerEffectiveVipCookie();
        REFRESH_EXECUTOR.execute(() -> {
            try {
                SongInfoData updated = QqMusicUtils.resolveSong(qqInput, serverVipCookie, quality);
                if (updated == null || updated.songUrl == null || updated.songUrl.isBlank()) {
                    return;
                }
                state.cachedSong = copySongInfo(updated);
            } catch (Exception e) {
                Netmusiccanneedqq.LOGGER.error("Failed to refresh QQ music url", e);
            } finally {
                state.inFlight.set(false);
            }
        });
    }

    private static void applyCachedSongInfo(ItemStack stack, ItemMusicCD.SongInfo target, SongInfoData source) {
        boolean changed = false;
        if (hasText(source.songUrl) && !source.songUrl.equals(target.songUrl)) {
            target.songUrl = source.songUrl;
            changed = true;
        }
        if (source.songTime > 0 && source.songTime != target.songTime) {
            target.songTime = source.songTime;
            changed = true;
        }
        if (hasText(source.songName) && !source.songName.equals(target.songName)) {
            target.songName = source.songName;
            changed = true;
        }
        if (changed) {
            ItemMusicCD.setSongInfo(target, stack);
        }
    }

    private static SongInfoData copySongInfo(SongInfoData source) {
        SongInfoData copy = new SongInfoData();
        copy.songUrl = source.songUrl;
        copy.songName = source.songName;
        copy.songTime = source.songTime;
        copy.transName = source.transName;
        copy.vip = source.vip;
        copy.readOnly = source.readOnly;
        if (source.artists != null) {
            copy.artists.addAll(source.artists);
        }
        return copy;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static final class RefreshState {
        private final AtomicBoolean inFlight = new AtomicBoolean(false);
        private volatile long lastRequestAt;
        private volatile SongInfoData cachedSong;
    }

    private static final class RefreshThreadFactory implements ThreadFactory {
        private int index = 1;

        @Override
        public synchronized Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "netmusiccanneedqq-song-refresh-" + index++);
            thread.setDaemon(true);
            return thread;
        }
    }
}
