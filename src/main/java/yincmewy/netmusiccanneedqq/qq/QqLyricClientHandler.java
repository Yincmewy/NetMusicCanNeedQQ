package yincmewy.netmusiccanneedqq.qq;

import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端处理器，用于存储和获取QQ音乐歌词
 * 当收到QqLyricMessage时存储songmid，当NetMusicSound创建时获取歌词
 */
@OnlyIn(Dist.CLIENT)
public final class QqLyricClientHandler {
    // 存储songmid，key为方块位置
    private static final ConcurrentHashMap<BlockPos, String> SONGMID_MAP = new ConcurrentHashMap<>();
    // 存储已获取的歌词，key为方块位置
    private static final ConcurrentHashMap<BlockPos, LyricRecord> LYRIC_MAP = new ConcurrentHashMap<>();
    // 歌曲名称缓存，用于歌词解析
    private static final ConcurrentHashMap<BlockPos, String> SONG_NAME_MAP = new ConcurrentHashMap<>();

    private static final long EXPIRE_TIME_MS = 30_000; // 30秒过期

    private QqLyricClientHandler() {
    }

    /**
     * 存储songmid（客户端收到QqLyricMessage时调用）
     */
    public static void storeSongmid(BlockPos pos, String songmid) {
        if (songmid != null && !songmid.isBlank()) {
            SONGMID_MAP.put(pos, songmid);
        }
    }

    /**
     * 设置歌曲名称（用于歌词解析）
     */
    public static void setSongName(BlockPos pos, String songName) {
        if (songName != null && !songName.isBlank()) {
            SONG_NAME_MAP.put(pos, songName);
        }
    }

    /**
     * 获取并移除存储的songmid
     *
     * @return songmid 或 null
     */
    public static String getAndRemoveSongmid(BlockPos pos) {
        return SONGMID_MAP.remove(pos);
    }

    /**
     * 获取songmid（不移除）
     */
    public static String getSongmid(BlockPos pos) {
        return SONGMID_MAP.get(pos);
    }

    /**
     * 存储已获取的歌词
     */
    public static void storeLyric(BlockPos pos, LyricRecord lyric) {
        if (lyric != null) {
            LYRIC_MAP.put(pos, lyric);
        }
    }

    /**
     * 获取并移除存储的歌词
     *
     * @return LyricRecord 或 null
     */
    public static LyricRecord getAndRemoveLyric(BlockPos pos) {
        return LYRIC_MAP.remove(pos);
    }

    /**
     * 获取歌词（不移除）
     */
    public static LyricRecord getLyric(BlockPos pos) {
        return LYRIC_MAP.get(pos);
    }

    /**
     * 获取歌曲名称
     */
    public static String getSongName(BlockPos pos) {
        return SONG_NAME_MAP.get(pos);
    }

    /**
     * 清理过期的数据
     */
    public static void cleanup() {
        if (SONGMID_MAP.size() > 100) {
            SONGMID_MAP.clear();
        }
        if (LYRIC_MAP.size() > 100) {
            LYRIC_MAP.clear();
        }
        if (SONG_NAME_MAP.size() > 100) {
            SONG_NAME_MAP.clear();
        }
    }
}
