package yincmewy.netmusiccanneedqq.qq;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QqLoginService {
    private static final String APPID = "716027609";
    private static final String THIRD_APPID = "100497308";
    private static final String REDIRECT_URI = "https://y.qq.com/wk_v17/common_login.html?type=QQ&&redirect=";
    private static final String QR_SHOW_URL = "https://xui.ptlogin2.qq.com/ssl/ptqrshow";
    private static final String QR_LOGIN_URL = "https://xui.ptlogin2.qq.com/ssl/ptqrlogin";
    private static final String AUTHORIZE_URL = "https://graph.qq.com/oauth2.0/authorize";
    private static final String MUSICU_URL = "https://u6.y.qq.com/cgi-bin/musicu.fcg";
    private static final Pattern PTUI_CB = Pattern.compile("ptuiCB\\('(\\d+)','[^']*','([^']*)','[^']*','([^']*)'");
    private static final Pattern CODE_PATTERN = Pattern.compile("code=([^&]+)");

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "netmusiccanneedqq-login-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    public enum LoginState {
        IDLE, FETCHING_QR, WAITING_SCAN, AUTHORIZING, LOGGING_IN, SUCCESS, FAILED, QR_EXPIRED
    }

    private QqLoginService() {
    }

    public static CompletableFuture<byte[]> fetchQrCode() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder(QR_SHOW_URL);
                urlBuilder.append("?appid=").append(APPID);
                urlBuilder.append("&e=2&l=M&s=3&d=72&v=4&t=0.787&daid=383");
                urlBuilder.append("&pt_3rd_aid=").append(THIRD_APPID);
                urlBuilder.append("&u1=").append(URLEncoder.encode("https://graph.qq.com/oauth2.0/login_jump", StandardCharsets.UTF_8));

                HttpURLConnection conn = (HttpURLConnection) new URL(urlBuilder.toString()).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setInstanceFollowRedirects(true);
                conn.connect();

                String qrsig = null;
                for (Map.Entry<String, java.util.List<String>> entry : conn.getHeaderFields().entrySet()) {
                    if (!"Set-Cookie".equalsIgnoreCase(entry.getKey())) continue;
                    for (String cookieStr : entry.getValue()) {
                        String val = extractCookieValue(cookieStr, "qrsig");
                        if (val != null) {
                            qrsig = val;
                            break;
                        }
                    }
                    if (qrsig != null) break;
                }

                byte[] imageData = conn.getInputStream().readAllBytes();
                conn.disconnect();

                if (qrsig == null || qrsig.isBlank()) {
                    Netmusiccanneedqq.LOGGER.error("Failed to extract qrsig from QR code response cookies");
                }
                Netmusiccanneedqq.LOGGER.info("QR code fetched, qrsig present: {}", qrsig != null);
                QrSession.set(qrsig);
                return imageData;
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch QR code", e);
            }
        }, EXECUTOR);
    }

    public static CompletableFuture<LoginState> pollLogin() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String qrsig = QrSession.getQrsig();
                if (qrsig == null || qrsig.isBlank()) {
                    Netmusiccanneedqq.LOGGER.error("pollLogin: qrsig is null/blank, cannot poll");
                    return LoginState.FAILED;
                }
                long ptqrtoken = calculatePtqrtoken(qrsig);

                StringBuilder urlBuilder = new StringBuilder(QR_LOGIN_URL);
                urlBuilder.append("?u1=").append(URLEncoder.encode("https://graph.qq.com/oauth2.0/login_jump", StandardCharsets.UTF_8));
                urlBuilder.append("&ptqrtoken=").append(ptqrtoken);
                urlBuilder.append("&ptredirect=0&h=1&t=1&g=1&from_ui=1&ptlang=2052");
                urlBuilder.append("&js_ver=25072815&js_type=1&login_sig=&pt_uistyle=40");
                urlBuilder.append("&aid=").append(APPID);
                urlBuilder.append("&daid=383");
                urlBuilder.append("&pt_3rd_aid=").append(THIRD_APPID);

                HttpURLConnection conn = (HttpURLConnection) new URL(urlBuilder.toString()).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Cookie", "qrsig=" + qrsig);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                String body = readResponse(conn);
                conn.disconnect();

                Netmusiccanneedqq.LOGGER.debug("pollLogin response: {}", body);

                Matcher m = PTUI_CB.matcher(body);
                if (!m.find()) {
                    Netmusiccanneedqq.LOGGER.warn("pollLogin: ptuiCB not found in response");
                    return LoginState.WAITING_SCAN;
                }

                String code = m.group(1);
                Netmusiccanneedqq.LOGGER.info("pollLogin: status code = {}", code);

                if ("65".equals(code)) {
                    return LoginState.QR_EXPIRED;
                }
                if (!"0".equals(code)) {
                    return LoginState.WAITING_SCAN;
                }

                String checkSigUrl = m.group(2);
                if (checkSigUrl == null || checkSigUrl.isBlank()) {
                    checkSigUrl = m.group(3);
                }

                return processLoginSuccess(checkSigUrl);
            } catch (Exception e) {
                Netmusiccanneedqq.LOGGER.error("Login poll failed", e);
                return LoginState.FAILED;
            }
        }, EXECUTOR);
    }

    private static LoginState processLoginSuccess(String checkSigUrl) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(checkSigUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.connect();

            String uin = null;
            String ptOauthToken = null;
            String pSkey = null;

            for (Map.Entry<String, java.util.List<String>> entry : conn.getHeaderFields().entrySet()) {
                if (!"Set-Cookie".equalsIgnoreCase(entry.getKey())) {
                    continue;
                }
                for (String cookieStr : entry.getValue()) {
                    String val;
                    if ((val = extractCookieValue(cookieStr, "pt2gguin")) != null) uin = val;
                    if ((val = extractCookieValue(cookieStr, "pt_oauth_token")) != null) ptOauthToken = val;
                    if ((val = extractCookieValue(cookieStr, "p_skey")) != null) pSkey = val;
                }
            }
            conn.disconnect();

            if (uin == null || ptOauthToken == null || pSkey == null) {
                Netmusiccanneedqq.LOGGER.error("Missing login cookies: uin={}, token={}, skey={}", uin != null, ptOauthToken != null, pSkey != null);
                return LoginState.FAILED;
            }

            String authCode = authorize(uin, ptOauthToken, pSkey);
            if (authCode == null) {
                return LoginState.FAILED;
            }

            QqCredential cred = loginServer(uin, authCode);
            if (cred == null) {
                return LoginState.FAILED;
            }

            QqCredentialManager.save(cred);
            Netmusiccanneedqq.LOGGER.info("QQ Music login successful, musicid={}", cred.getMusicId());
            return LoginState.SUCCESS;
        } catch (Exception e) {
            Netmusiccanneedqq.LOGGER.error("Login processing failed", e);
            return LoginState.FAILED;
        }
    }

    private static String authorize(String uin, String ptOauthToken, String pSkey) throws IOException {
        long gTk = calculateGtk(pSkey);

        Map<String, String> formData = new LinkedHashMap<>();
        formData.put("response_type", "code");
        formData.put("client_id", THIRD_APPID);
        formData.put("redirect_uri", REDIRECT_URI);
        formData.put("scope", "get_user_info");
        formData.put("state", "y_new.top.pop.logout");
        formData.put("switch", "");
        formData.put("from_ptlogin", "1");
        formData.put("src", "1");
        formData.put("update_auth", "1");
        formData.put("openapi", "1010");
        formData.put("g_tk", String.valueOf(gTk));
        formData.put("auth_time", String.valueOf(System.currentTimeMillis() / 1000));

        StringBuilder formBody = new StringBuilder();
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            if (formBody.length() > 0) formBody.append('&');
            formBody.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            formBody.append('=');
            formBody.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(AUTHORIZE_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setInstanceFollowRedirects(false);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Cookie", "p_uin=" + uin + "; pt_oauth_token=" + ptOauthToken + "; p_skey=" + pSkey);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(formBody.toString().getBytes(StandardCharsets.UTF_8));
        }

        String location = conn.getHeaderField("Location");
        conn.disconnect();

        if (location == null) {
            Netmusiccanneedqq.LOGGER.error("Authorization failed: no redirect location");
            return null;
        }

        Matcher m = CODE_PATTERN.matcher(location);
        if (!m.find()) {
            Netmusiccanneedqq.LOGGER.error("Authorization failed: no code in redirect");
            return null;
        }
        return m.group(1);
    }

    private static QqCredential loginServer(String uin, String code) throws IOException {
        JsonObject comm = new JsonObject();
        comm.addProperty("_channelid", "208");
        comm.addProperty("_os_version", "6.2.9200-2");
        comm.addProperty("authst", "");
        comm.addProperty("ct", "19");
        comm.addProperty("cv", "2121");
        comm.addProperty("guid", "");
        comm.addProperty("patch", "118");
        comm.addProperty("tmeAppID", "qqmusic");
        comm.addProperty("tmeLoginType", 2);
        comm.addProperty("uin", uin);

        JsonObject param = new JsonObject();
        param.addProperty("appid", Integer.parseInt(THIRD_APPID));
        param.addProperty("code", code);
        param.addProperty("deviceName", "minecraft");
        param.addProperty("forceRefreshToken", 0);
        param.addProperty("onlyNeedAccessToken", 0);

        JsonObject loginReq = new JsonObject();
        loginReq.addProperty("method", "Login");
        loginReq.addProperty("module", "music.login.LoginServer");
        loginReq.add("param", param);

        JsonObject body = new JsonObject();
        body.add("comm", comm);
        body.add("music.login.LoginServer.Login", loginReq);

        String url = MUSICU_URL + "?pcachetime=" + System.currentTimeMillis() / 1000;
        String response = postJson(url, body.toString());

        JsonObject tree = JsonParser.parseString(response).getAsJsonObject();
        JsonObject loginResp = tree.getAsJsonObject("music.login.LoginServer.Login");
        if (loginResp == null || loginResp.get("code").getAsInt() != 0) {
            Netmusiccanneedqq.LOGGER.error("Login server returned error");
            return null;
        }

        JsonObject data = loginResp.getAsJsonObject("data");
        String musicId = data.has("musicid") ? data.get("musicid").getAsString() : "";
        String musicKey = data.has("musickey") ? data.get("musickey").getAsString() : "";
        long keyExpiresIn = data.has("keyExpiresIn") ? data.get("keyExpiresIn").getAsLong() : 0;
        long createTime = data.has("musickeyCreateTime") ? data.get("musickeyCreateTime").getAsLong() : System.currentTimeMillis() / 1000;
        String refreshKey = data.has("refresh_key") ? data.get("refresh_key").getAsString() : "";
        String refreshToken = data.has("refresh_token") ? data.get("refresh_token").getAsString() : "";

        return new QqCredential(musicId, musicKey, keyExpiresIn, createTime, refreshKey, refreshToken);
    }

    static long calculatePtqrtoken(String qrsig) {
        long e = 0;
        for (int i = 0; i < qrsig.length(); i++) {
            e += (e << 5) + qrsig.charAt(i);
            e &= 0x7FFFFFFF;
        }
        return e;
    }

    static long calculateGtk(String skey) {
        long hash = 5381;
        for (int i = 0; i < skey.length(); i++) {
            hash += (hash << 5) + skey.charAt(i);
            hash &= 0x7FFFFFFF;
        }
        return hash & 0x7FFFFFFF;
    }

    private static String extractCookieValue(String cookieStr, String name) {
        if (cookieStr == null) return null;
        Pattern p = Pattern.compile(Pattern.quote(name) + "=([^;]+)");
        Matcher m = p.matcher(cookieStr);
        return m.find() ? m.group(1) : null;
    }

    private static String readResponse(HttpURLConnection conn) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static String postJson(String url, String jsonBody) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
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

        String response = readResponse(conn);
        conn.disconnect();
        return response;
    }

    public static final class QrSession {
        private static volatile String qrsig;
        private static volatile Consumer<LoginState> stateListener;

        private QrSession() {
        }

        static void set(String sig) {
            qrsig = sig;
        }

        static String getQrsig() {
            return qrsig;
        }

        public static void setStateListener(Consumer<LoginState> listener) {
            stateListener = listener;
        }

        public static void notifyState(LoginState state) {
            Consumer<LoginState> l = stateListener;
            if (l != null) {
                l.accept(state);
            }
        }

        public static void reset() {
            qrsig = null;
        }
    }
}
