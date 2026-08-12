package yincmewy.netmusiccanneedqq.qq;

import com.github.tartaricacid.netmusic.api.NetWorker;
import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从QQ音乐API获取加密歌词并解密
 * 使用 lyric_download.fcg 接口获取加密的QRC歌词，通过TripleDES解密+zlib解压得到明文
 */
public final class QqLyricFetcher {
    private static final String LYRIC_ENCRYPTED_API_URL = "https://c.y.qq.com/qqmusic/fcgi-bin/lyric_download.fcg";

    private static final Pattern CONTENT_PATTERN = Pattern.compile("<content[^>]*>(.*?)</content>", Pattern.DOTALL);
    private static final Pattern CONTENTTS_PATTERN = Pattern.compile("<contentts[^>]*>(.*?)</contentts>", Pattern.DOTALL);

    private static final Pattern CDATA_PATTERN = Pattern.compile("<!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);
    private static final Pattern LYRIC_CONTENT_PATTERN = Pattern.compile("LyricContent=\"([^\"]*)\"", Pattern.DOTALL);
    private static final Pattern LRC_PATTERN = Pattern.compile("\\[(\\d+):(\\d+)[.:](\\d+)]([^\\[]*)");

    private QqLyricFetcher() {
    }

    /**
     * 从QQ音乐API获取歌词
     *
     * @param songmid  歌曲的songmid
     * @param songName 歌曲名称，用于在歌词开头显示
     * @return LyricRecord 或 null（如果获取失败）
     */
    public static LyricRecord fetchLyric(String songmid, String songName) {
        if (songmid == null || songmid.isBlank()) {
            return null;
        }
        try {
            // 第一步：将songmid转换为数字ID
            int songId = QqMusicUtils.getSongIdByMid(songmid);
            if (songId <= 0) {
                Netmusiccanneedqq.LOGGER.warn("Failed to get songId for songmid: {}", songmid);
                return null;
            }

            String encryptedResponse = fetchEncryptedLyric(songId);
            if (encryptedResponse == null || encryptedResponse.isBlank()) {
                return null;
            }

            String origText = extractTagContentFromResponse(encryptedResponse, CONTENT_PATTERN, songId, "content");
            String transText = extractTagContentFromResponse(encryptedResponse, CONTENTTS_PATTERN, songId, "contentts");

            if (origText == null || origText.isBlank()) {
                Netmusiccanneedqq.LOGGER.warn("No original lyrics found for songId: {}", songId);
                return null;
            }

            String origLrc = QrcToLrcConverter.convertToLrc(origText);
            Netmusiccanneedqq.LOGGER.info("Converted orig LRC for songId: {}, first 200 chars: {}",
                    songId, origLrc.length() > 200 ? origLrc.substring(0, 200) : origLrc);

            Int2ObjectSortedMap<String> origMap = parseLrcToMap(origLrc);

            Int2ObjectSortedMap<String> transMap = null;
            if (transText != null && !transText.isBlank()) {
                Netmusiccanneedqq.LOGGER.info("Parsing trans directly for songId: {}, first 200 chars: {}",
                        songId, transText.length() > 200 ? transText.substring(0, 200) : transText);
                transMap = parseLrcToMap(transText);
                Netmusiccanneedqq.LOGGER.info("Parsed trans lines: {}", transMap.size());
            }

            return buildLyricRecord(origMap, transMap, songName);
        } catch (Exception e) {
            Netmusiccanneedqq.LOGGER.error("Failed to fetch QQ lyric for songmid: {}", songmid, e);
            return null;
        }
    }

    /**
     * 从lyric_download.fcg获取加密的歌词XML
     */
    private static String fetchEncryptedLyric(int songId) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("Referer", "https://c.y.qq.com/");
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
            headers.put("Cookie", "NMTID=" + java.util.UUID.randomUUID());

            String postData = "version=15&miniversion=82&lrctype=4&musicid=" + songId;
            return httpPost(LYRIC_ENCRYPTED_API_URL, postData, headers);
        } catch (Exception e) {
            Netmusiccanneedqq.LOGGER.error("Failed to fetch encrypted lyric for songId: {}", songId, e);
            return null;
        }
    }

    private static String extractTagContentFromResponse(String response, Pattern pattern, int songId, String tagName) {
        response = response.replace("<!--", "").replace("-->", "");
        return extractTagContent(response, pattern, songId, tagName);
    }

    private static String extractTagContent(String response, Pattern pattern, int songId, String tagName) {
        Matcher matcher = pattern.matcher(response);
        if (!matcher.find()) {
            Netmusiccanneedqq.LOGGER.info("Tag <{}> not found in response for songId: {}", tagName, songId);
            return null;
        }

        String text = matcher.group(1).trim();

        Matcher cdataMatcher = CDATA_PATTERN.matcher(text);
        if (cdataMatcher.find()) {
            text = cdataMatcher.group(1).trim();
        }

        if (text.isEmpty()) {
            Netmusiccanneedqq.LOGGER.info("Tag <{}> is empty for songId: {}", tagName, songId);
            return null;
        }

        Netmusiccanneedqq.LOGGER.info("Found <{}> content for songId: {}, length: {}, isHex: {}",
                tagName, songId, text.length(), QqLyricDecrypter.isHex(text));

        String decrypted = decryptContent(text, songId);
        if (decrypted == null || decrypted.isBlank()) {
            return null;
        }

        Netmusiccanneedqq.LOGGER.info("Tag <{}> final content for songId: {}, first 300 chars: {}",
                tagName, songId, decrypted.length() > 300 ? decrypted.substring(0, 300) : decrypted);

        if (decrypted.contains("<?xml") || decrypted.contains("LyricContent")) {
            String lyricContent = extractLyricContent(decrypted);
            if (lyricContent != null && !lyricContent.isBlank()) {
                return lyricContent;
            }
        }

        return decrypted;
    }

    private static String decryptContent(String text, int songId) {
        if (!QqLyricDecrypter.isHex(text)) {
            Netmusiccanneedqq.LOGGER.info("Content is not hex, treating as plaintext for songId: {}", songId);
            return text;
        }

        String decrypted = QqLyricDecrypter.decryptLyrics(text);
        if (decrypted == null) {
            Netmusiccanneedqq.LOGGER.warn("Failed to decrypt lyrics for songId: {}", songId);
            return null;
        }

        Netmusiccanneedqq.LOGGER.info("Decrypted lyrics for songId: {}, first 200 chars: {}",
                songId, decrypted.length() > 200 ? decrypted.substring(0, 200) : decrypted);
        return decrypted;
    }

    private static String extractLyricContent(String xmlText) {
        Matcher matcher = LYRIC_CONTENT_PATTERN.matcher(xmlText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static LyricRecord buildLyricRecord(Int2ObjectSortedMap<String> origMap, Int2ObjectSortedMap<String> transMap, String songName) {
        if (origMap.isEmpty()) {
            Netmusiccanneedqq.LOGGER.warn("No original LRC lyrics parsed");
            return null;
        }

        if (!origMap.containsKey(0)) {
            origMap.put(0, songName != null ? songName : "");
        }

        if (transMap != null && !transMap.isEmpty()) {
            Netmusiccanneedqq.LOGGER.info("Parsed bilingual lyrics: {} original, {} translated lines", origMap.size(), transMap.size());
            return new LyricRecord(origMap, transMap);
        } else {
            Netmusiccanneedqq.LOGGER.info("Parsed {} original lyric lines (no translation)", origMap.size());
            return new LyricRecord(origMap);
        }
    }

    private static Int2ObjectSortedMap<String> parseLrcToMap(String lrcContent) {
        Int2ObjectSortedMap<String> map = new Int2ObjectRBTreeMap<>();
        if (lrcContent == null || lrcContent.isBlank()) {
            return map;
        }

        Matcher matcher = LRC_PATTERN.matcher(lrcContent);
        while (matcher.find()) {
            int minutes = Integer.parseInt(matcher.group(1));
            int seconds = Integer.parseInt(matcher.group(2));
            int milliseconds = Integer.parseInt(matcher.group(3));
            String text = matcher.group(4).trim();

            if (text.isEmpty() || text.equals("//") || text.equals("/")) {
                continue;
            }

            int totalTick = ((minutes * 60 + seconds) * 1000 + milliseconds) / 50;
            map.put(totalTick, text);
        }
        return map;
    }

    private static String httpPost(String urlStr, String postData, Map<String, String> headers) {
        try {
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new URL(urlStr).openConnection(NetWorker.getProxyFromConfig());
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            Netmusiccanneedqq.LOGGER.info("HTTP POST {} -> response code: {}", urlStr, responseCode);

            java.io.InputStream inputStream;
            if (responseCode >= 400) {
                inputStream = connection.getErrorStream();
            } else {
                inputStream = connection.getInputStream();
            }

            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            }

            if (responseCode >= 400) {
                Netmusiccanneedqq.LOGGER.warn("HTTP POST error response: {}", result.toString());
                return null;
            }

            return result.toString();
        } catch (Exception e) {
            Netmusiccanneedqq.LOGGER.error("HTTP POST failed: {}", urlStr, e);
            return null;
        }
    }
}
