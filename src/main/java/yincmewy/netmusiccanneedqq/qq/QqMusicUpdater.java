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
    private static final long REQUEST_COOLDOWN_MS = 30 * 60 * 1000L;
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
        long now = System.currentTimeMillis();
        if (state.cachedSong != null && hasText(state.cachedSong.songUrl)) {
            applyCachedSongInfo(stack, info, state.cachedSong);
        }
        if (state.cachedSong == null || !hasText(state.cachedSong.songUrl)) {
            QualityLevel quality = QqDiscNbt.getQuality(stack);
            requestRefreshSync(qqInput, state, quality);
            if (state.cachedSong != null && hasText(state.cachedSong.songUrl)) {
                applyCachedSongInfo(stack, info, state.cachedSong);
            }
        } else if (now - state.lastRequestAt >= REQUEST_COOLDOWN_MS) {
            QualityLevel cooldownQuality = QqDiscNbt.getQuality(stack);
            requestRefreshAsync(qqInput, state, now, cooldownQuality);
        }
        return info;
    }

    // Non-blocking: update NBT in background if URL expired, don't wait
    public static void refreshIfNeededNonBlocking(ItemStack stack, ItemMusicCD.SongInfo info) {
        if (info == null || !QqDiscNbt.isQqDisc(stack)) {
            return;
        }
        String qqInput = QqDiscNbt.getQqInput(stack);
        if (qqInput == null || qqInput.isBlank()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) {
            return;
        }
        RefreshState state = REFRESH_STATES.computeIfAbsent(qqInput, ignored -> new RefreshState());
        if (state.cachedSong != null && hasText(state.cachedSong.songUrl)) {
            applyCachedSongInfo(stack, info, state.cachedSong);
        }
        if (state.cachedSong == null || !hasText(state.cachedSong.songUrl)) {
            // Trigger async refresh only
            if (state.inFlight.compareAndSet(false, true)) {
                QualityLevel quality = QqDiscNbt.getQuality(stack);
                state.lastRequestAt = System.currentTimeMillis();
                String serverVipCookie = VipCookieState.getServerEffectiveVipCookie();
                REFRESH_EXECUTOR.execute(() -> {
                    SongInfoData updated;
                    try {
                        updated = QqMusicUtils.resolveSong(qqInput, serverVipCookie, quality);
                    } catch (Exception e) {
                        Netmusiccanneedqq.LOGGER.error("Failed to refresh QQ music url (non-blocking)", e);
                        updated = null;
                    } finally {
                        state.inFlight.set(false);
                    }
                    if (updated != null && hasText(updated.songUrl)) {
                        state.cachedSong = copySongInfo(updated);
                    }
                });
            }
        }
    }

    private static void requestRefreshSync(String qqInput, RefreshState state, QualityLevel quality) {
        if (!state.inFlight.compareAndSet(false, true)) {
            // Another request is in-flight (e.g., from another player). Wait for it.
            waitForInFlight(state);
            return;
        }
        state.lastRequestAt = System.currentTimeMillis();
        String serverVipCookie = VipCookieState.getServerEffectiveVipCookie();
        SongInfoData updated;
        try {
            updated = QqMusicUtils.resolveSong(qqInput, serverVipCookie, quality);
        } catch (Exception e) {
            Netmusiccanneedqq.LOGGER.error("Failed to refresh QQ music url (sync)", e);
            updated = null;
        } finally {
            state.inFlight.set(false);
        }
        if (updated != null && hasText(updated.songUrl)) {
            state.cachedSong = copySongInfo(updated);
        }
    }

    private static void waitForInFlight(RefreshState state) {
        int waited = 0;
        while (state.inFlight.get()) {
            if (waited >= 5000) { // 5 second timeout
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            waited += 10;
        }
    }

    private static void requestRefreshAsync(String qqInput, RefreshState state, long now, QualityLevel quality) {
        if (!state.inFlight.compareAndSet(false, true)) {
            return;
        }
        state.lastRequestAt = now;
        String serverVipCookie = VipCookieState.getServerEffectiveVipCookie();
        REFRESH_EXECUTOR.execute(() -> {
            SongInfoData updated;
            try {
                updated = QqMusicUtils.resolveSong(qqInput, serverVipCookie, quality);
            } catch (Exception e) {
                Netmusiccanneedqq.LOGGER.error("Failed to refresh QQ music url", e);
                updated = null;
            } finally {
                state.inFlight.set(false);
            }
            if (updated != null && hasText(updated.songUrl)) {
                state.cachedSong = copySongInfo(updated);
            }
        });
    }

    private static void applyCachedSongInfo(ItemStack stack, ItemMusicCD.SongInfo target, SongInfoData source) {
        boolean changed = false;
        // Always update URL if we have a cached one
        if (hasText(source.songUrl)) {
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
