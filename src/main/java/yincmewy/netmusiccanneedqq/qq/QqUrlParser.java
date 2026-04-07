package yincmewy.netmusiccanneedqq.qq;

import yincmewy.netmusiccanneedqq.data.ParsedUrl;
import yincmewy.netmusiccanneedqq.data.ParsedUrl.ResourceType;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QqUrlParser {
    private static final Pattern ALBUM_DETAIL = Pattern.compile("albumDetail/([0-9a-zA-Z]+)");
    private static final Pattern SONG_DETAIL = Pattern.compile("songDetail/([0-9a-zA-Z]+)");
    private static final Pattern PLAYLIST_PATH = Pattern.compile("playlist/([0-9a-zA-Z]+)");
    private static final Pattern TAOGE_ID = Pattern.compile("id=([0-9a-zA-Z]+)");
    private static final Pattern SONG_MID_ONLY = Pattern.compile("^[0-9a-zA-Z]{10,16}$");

    private QqUrlParser() {
    }

    public static ParsedUrl parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        input = input.trim();

        if (SONG_MID_ONLY.matcher(input).matches()) {
            return new ParsedUrl(ResourceType.SONG, input);
        }

        String url = input;
        if (url.contains("c6.y.qq.com/base/fcgi-bin/u") || url.contains("c.y.qq.com")) {
            url = followRedirect(url);
            if (url == null) return null;
        }

        Matcher m;

        m = ALBUM_DETAIL.matcher(url);
        if (m.find()) {
            return new ParsedUrl(ResourceType.ALBUM, m.group(1));
        }

        if (url.contains("songDetail") || url.contains("song/")) {
            m = SONG_DETAIL.matcher(url);
            if (m.find()) {
                return new ParsedUrl(ResourceType.SONG, m.group(1));
            }
        }

        if (url.contains("playlist")) {
            m = PLAYLIST_PATH.matcher(url);
            if (m.find()) {
                return new ParsedUrl(ResourceType.PLAYLIST, m.group(1));
            }
        }

        if (url.contains("taoge.html") || url.contains("playsong.html")) {
            m = TAOGE_ID.matcher(url);
            if (m.find()) {
                String id = m.group(1);
                if (url.contains("taoge")) {
                    return new ParsedUrl(ResourceType.PLAYLIST, id);
                }
                return new ParsedUrl(ResourceType.SONG, id);
            }
        }

        return null;
    }

    private static String followRedirect(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.connect();
            String finalUrl = conn.getURL().toString();
            conn.disconnect();
            return finalUrl;
        } catch (Exception e) {
            return null;
        }
    }
}
