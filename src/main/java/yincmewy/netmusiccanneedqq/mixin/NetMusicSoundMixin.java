package yincmewy.netmusiccanneedqq.mixin;

import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.qq.QqLyricClientHandler;
import yincmewy.netmusiccanneedqq.qq.QqLyricFetcher;

import java.lang.reflect.Method;
import java.net.URL;

/**
 * Mixin到NetMusicSound，在构造函数完成后检查是否有QQ音乐歌词需要设置
 */
@Mixin(value = NetMusicSound.class, remap = false)
public abstract class NetMusicSoundMixin {
    @Shadow
    @Mutable
    private LyricRecord lyricRecord;

    @Shadow
    private BlockPos pos;

    /**
     * 兼容Java 17/21的异步执行方式
     * Java 21+ 使用虚拟线程，Java 17 使用普通守护线程
     */
    @Unique
    private static void netmusiccanneedqq$runAsync(Runnable task) {
        // 尝试使用Java 21的虚拟线程
        try {
            Method startVirtualThread = Thread.class.getMethod("startVirtualThread", Runnable.class);
            startVirtualThread.invoke(null, task);
            return;
        } catch (Exception ignored) {
            // Java 17不支持虚拟线程，回退到普通线程
        }
        // 回退：普通守护线程
        Thread thread = new Thread(task, "QQ-Lyric-Fetcher");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 在NetMusicSound构造函数完成后，检查是否有QQ音乐歌词
     * 如果原始歌词为空且有存储的QQ songmid，则获取歌词
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void netmusiccanneedqq$setQqLyric(BlockPos pos, URL songUrl, int timeSecond, LyricRecord lyricRecord, CallbackInfo ci) {
        // 如果已经有歌词（网易云），不需要获取QQ歌词
        if (this.lyricRecord != null) {
            return;
        }

        // 尝试获取已缓存的歌词
        LyricRecord cachedLyric = QqLyricClientHandler.getAndRemoveLyric(pos);
        if (cachedLyric != null) {
            this.lyricRecord = cachedLyric;
            Netmusiccanneedqq.LOGGER.info("Applied cached QQ lyric for pos: {}", pos);
            return;
        }

        // 尝试获取songmid并异步获取歌词
        String songmid = QqLyricClientHandler.getAndRemoveSongmid(pos);
        if (songmid == null || songmid.isBlank()) {
            return;
        }

        String songName = QqLyricClientHandler.getSongName(pos);
        Netmusiccanneedqq.LOGGER.info("Fetching QQ lyric for songmid: {} at pos: {}", songmid, pos);

        // 异步获取歌词
        final String finalSongName = songName != null ? songName : "";
        netmusiccanneedqq$runAsync(() -> {
            try {
                LyricRecord qqLyric = QqLyricFetcher.fetchLyric(songmid, finalSongName);
                if (qqLyric != null) {
                    // 直接设置到当前实例（使用@Mutable使final字段可修改）
                    this.lyricRecord = qqLyric;
                    Netmusiccanneedqq.LOGGER.info("Successfully fetched QQ lyric for songmid: {}", songmid);
                } else {
                    Netmusiccanneedqq.LOGGER.warn("No lyric found for songmid: {}", songmid);
                }
            } catch (Exception e) {
                Netmusiccanneedqq.LOGGER.error("Failed to fetch QQ lyric for songmid: {}", songmid, e);
            }
        });
    }
}
