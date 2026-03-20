package yincmewy.netmusiccanneedqq.qq;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.Util;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.data.SongInfoData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class QqMusicApi {
    private static final String API_URL = "https://u6.y.qq.com/cgi-bin/musicu.fcg";

    private QqMusicApi() {
    }

    public static CompletableFuture<List<SongInfoData>> fetchAlbumSongs(String albumMid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject param = new JsonObject();
                param.addProperty("begin", 0);
                param.addProperty("num", 500);
                param.addProperty("order", 1);
                param.addProperty("albumMid", albumMid);

                JsonObject body = buildRequest("GetAlbumSongList", "music.musichallAlbum.AlbumSongList", param);
                String response = postJson(API_URL, body.toString());
                JsonObject tree = JsonParser.parseString(response).getAsJsonObject();

                JsonArray songList = tree.getAsJsonObject("music")
                        .getAsJsonObject("data")
                        .getAsJsonArray("songList");

                return parseSongList(songList, true);
            } catch (Exception e) {
                Netmusiccanneedqq.LOGGER.error("Failed to fetch album songs", e);
                return Collections.emptyList();
            }
        }, Util.backgroundExecutor());
    }

    public static CompletableFuture<List<SongInfoData>> fetchPlaylistSongs(String playlistId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject param = new JsonObject();
                try {
                    param.addProperty("disstid", Long.parseLong(playlistId));
                } catch (NumberFormatException e) {
                    param.addProperty("disstid", playlistId);
                }
                param.addProperty("userinfo", 1);
                param.addProperty("tag", 1);
                param.addProperty("is_pc", 1);

                JsonObject body = buildRequest("uniform_get_Dissinfo", "music.srfDissInfo.aiDissInfo", param);
                String response = postJson(API_URL, body.toString());
                JsonObject tree = JsonParser.parseString(response).getAsJsonObject();

                JsonArray songList = tree.getAsJsonObject("music")
                        .getAsJsonObject("data")
                        .getAsJsonArray("songlist");

                return parseSongList(songList, false);
            } catch (Exception e) {
                Netmusiccanneedqq.LOGGER.error("Failed to fetch playlist songs", e);
                return Collections.emptyList();
            }
        }, Util.backgroundExecutor());
    }

    public static CompletableFuture<SongInfoData> fetchSongDetail(String songMid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject param = new JsonObject();
                JsonArray mids = new JsonArray();
                mids.add(songMid);
                param.add("mids", mids);
                param.add("ids", new JsonArray());
                JsonArray types = new JsonArray();
                types.add(0);
                param.add("types", types);

                JsonObject body = buildRequest("GetTrackInfo", "music.trackInfo.UniformRuleCtrl", param);
                String response = postJson(API_URL, body.toString());
                JsonObject tree = JsonParser.parseString(response).getAsJsonObject();

                JsonArray tracks = tree.getAsJsonObject("music")
                        .getAsJsonObject("data")
                        .getAsJsonArray("tracks");

                if (tracks == null || tracks.isEmpty()) {
                    return null;
                }

                return parseSongInfo(tracks.get(0).getAsJsonObject());
            } catch (Exception e) {
                Netmusiccanneedqq.LOGGER.error("Failed to fetch song detail", e);
                return null;
            }
        }, Util.backgroundExecutor());
    }

    private static JsonObject buildRequest(String method, String module, JsonObject param) {
        JsonObject comm = new JsonObject();
        comm.addProperty("ct", "19");
        comm.addProperty("cv", "2121");

        JsonObject music = new JsonObject();
        music.addProperty("method", method);
        music.addProperty("module", module);
        music.add("param", param);

        JsonObject body = new JsonObject();
        body.add("comm", comm);
        body.add("music", music);
        return body;
    }

    private static List<SongInfoData> parseSongList(JsonArray songArray, boolean wrapped) {
        if (songArray == null) {
            return Collections.emptyList();
        }
        List<SongInfoData> result = new ArrayList<>();
        for (JsonElement element : songArray) {
            JsonObject songObj = element.getAsJsonObject();
            if (wrapped && songObj.has("songInfo")) {
                songObj = songObj.getAsJsonObject("songInfo");
            }
            SongInfoData info = parseSongInfo(songObj);
            if (info != null) {
                result.add(info);
            }
        }
        return result;
    }

    private static SongInfoData parseSongInfo(JsonObject song) {
        try {
            SongInfoData data = new SongInfoData();
            data.songName = song.has("name") ? song.get("name").getAsString() : "";
            data.songTime = song.has("interval") ? song.get("interval").getAsInt() : 0;

            String mid = song.has("mid") ? song.get("mid").getAsString() : "";
            data.songUrl = mid;

            if (song.has("singer")) {
                JsonArray singers = song.getAsJsonArray("singer");
                List<String> artistNames = new ArrayList<>();
                for (JsonElement s : singers) {
                    String name = s.getAsJsonObject().get("name").getAsString();
                    artistNames.add(name);
                }
                data.artists.addAll(artistNames);
            }

            if (song.has("pay")) {
                JsonObject pay = song.getAsJsonObject("pay");
                data.vip = pay.has("pay_play") && pay.get("pay_play").getAsInt() == 1;
            }

            return data;
        } catch (Exception e) {
            Netmusiccanneedqq.LOGGER.error("Failed to parse song info", e);
            return null;
        }
    }

    private static String postJson(String urlStr, String jsonBody) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        conn.disconnect();
        return sb.toString();
    }
}
