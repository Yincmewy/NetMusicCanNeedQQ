package yincmewy.netmusiccanneedqq.qq;

import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.api.NetWorker;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从QQ音乐API获取歌词
 * API: https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid={songmid}&format=json&nobase64=1
 */
public final class QqLyricFetcher {
    private static final String LYRIC_API_URL = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg";
    private static final Pattern LRC_PATTERN = Pattern.compile("\\[(\\d+):(\\d+)[.:](\\d+)](.*)");

    private QqLyricFetcher() {
    }

    /**
     * 从QQ音乐API获取歌词
     *
     * @param songmid 歌曲的songmid
     * @param songName 歌曲名称，用于在歌词开头显示
     * @return LyricRecord 或 null（如果获取失败）
     */
    public static LyricRecord fetchLyric(String songmid, String songName) {
        if (songmid == null || songmid.isBlank()) {
            return null;
        }
        try {
            String url = LYRIC_API_URL + "?songmid=" + songmid + "&format=json&nobase64=1";
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", "https://y.qq.com/portal/player.html");
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0");

            String response = httpGet(url, headers);
            if (response == null || response.isBlank()) {
                return null;
            }

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            int retcode = json.has("retcode") ? json.get("retcode").getAsInt() : -1;
            if (retcode != 0) {
                Netmusiccanneedqq.LOGGER.warn("QQ lyric API returned retcode: {}", retcode);
                return null;
            }

            String lyricStr = json.has("lyric") ? json.get("lyric").getAsString() : "";
            if (lyricStr.isBlank()) {
                return null;
            }

            return parseLyric(lyricStr, songName);
        } catch (Exception e) {
            Netmusiccanneedqq.LOGGER.error("Failed to fetch QQ lyric for songmid: {}", songmid, e);
            return null;
        }
    }

    /**
     * 解析LRC格式歌词
     */
    private static LyricRecord parseLyric(String lrcContent, String songName) {
        Int2ObjectSortedMap<String> lyrics = new Int2ObjectRBTreeMap<>();

        String[] lines = lrcContent.split("\n");
        for (String line : lines) {
            Matcher matcher = LRC_PATTERN.matcher(line.trim());
            if (matcher.find()) {
                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));
                int milliseconds = Integer.parseInt(matcher.group(3));
                String text = matcher.group(4).trim();
                // 转换为tick单位（1 tick = 50ms）
                int totalTick = ((minutes * 60 + seconds) * 1000 + milliseconds) / 50;
                lyrics.put(totalTick, text);
            }
        }

        if (lyrics.isEmpty()) {
            return null;
        }

        // 如果第 0 tick 没有歌词，则添加歌曲名称作为第一行歌词
        if (!lyrics.containsKey(0)) {
            lyrics.put(0, songName != null ? songName : "");
        }

        return new LyricRecord(lyrics);
    }

    /**
     * 发送HTTP GET请求
     */
    private static String httpGet(String urlStr, Map<String, String> headers) {
        try {
            URLConnection connection = new URL(urlStr).openConnection(NetWorker.getProxyFromConfig());
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            }
            return result.toString();
        } catch (Exception e) {
            Netmusiccanneedqq.LOGGER.error("HTTP GET failed: {}", urlStr, e);
            return null;
        }
    }
}
