package yincmewy.netmusiccanneedqq.mixin;

import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    @Unique
    private boolean netmusiccanneedqq$lyricFetchStarted = false;

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
        netmusiccanneedqq$lyricFetchStarted = true;

        // 异步获取歌词
        final String finalSongName = songName != null ? songName : "";
        Thread.startVirtualThread(() -> {
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

    /**
     * 在每个tick时检查是否有延迟到达的QQ歌词
     * 处理QqLyricMessage在NetMusicSound创建之后到达的情况
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void netmusiccanneedqq$checkDelayedLyric(CallbackInfo ci) {
        // 如果已经有歌词，不需要检查
        if (this.lyricRecord != null) {
            return;
        }

        // 如果已经开始获取歌词，不需要重复检查
        if (netmusiccanneedqq$lyricFetchStarted) {
            return;
        }

        // 检查是否有延迟到达的songmid
        String songmid = QqLyricClientHandler.getSongmid(this.pos);
        if (songmid == null || songmid.isBlank()) {
            return;
        }

        Netmusiccanneedqq.LOGGER.info("Delayed QQ lyric fetch for songmid: {} at pos: {}", songmid, this.pos);
        netmusiccanneedqq$lyricFetchStarted = true;
        QqLyricClientHandler.getAndRemoveSongmid(this.pos); // 移除已使用的songmid

        String songName = QqLyricClientHandler.getSongName(this.pos);
        final String finalSongName = songName != null ? songName : "";

        // 异步获取歌词
        Thread.startVirtualThread(() -> {
            try {
                LyricRecord qqLyric = QqLyricFetcher.fetchLyric(songmid, finalSongName);
                if (qqLyric != null) {
                    this.lyricRecord = qqLyric;
                    Netmusiccanneedqq.LOGGER.info("Successfully fetched delayed QQ lyric for songmid: {}", songmid);
                }
            } catch (Exception e) {
                Netmusiccanneedqq.LOGGER.error("Failed to fetch delayed QQ lyric for songmid: {}", songmid, e);
            }
        });
    }
}
