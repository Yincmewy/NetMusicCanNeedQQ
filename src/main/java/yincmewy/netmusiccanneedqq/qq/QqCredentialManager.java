package yincmewy.netmusiccanneedqq.qq;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class QqCredentialManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile QqCredential credential;
    private static volatile Path credentialFile;

    private QqCredentialManager() {
    }

    public static void init(Path configDir) {
        Path dir = configDir.resolve("netmusiccanneedqq");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            Netmusiccanneedqq.LOGGER.error("Failed to create config directory", e);
        }
        credentialFile = dir.resolve("credential.json");
        load();
    }

    public static void load() {
        if (credentialFile == null || !Files.exists(credentialFile)) {
            credential = null;
            return;
        }
        try (Reader reader = Files.newBufferedReader(credentialFile, StandardCharsets.UTF_8)) {
            credential = GSON.fromJson(reader, QqCredential.class);
        } catch (Exception e) {
            Netmusiccanneedqq.LOGGER.error("Failed to load credential", e);
            credential = null;
        }
    }

    public static void save(QqCredential cred) {
        credential = cred;
        if (credentialFile == null) {
            return;
        }
        try {
            Files.createDirectories(credentialFile.getParent());
            try (Writer writer = Files.newBufferedWriter(credentialFile, StandardCharsets.UTF_8)) {
                GSON.toJson(cred, writer);
            }
        } catch (IOException e) {
            Netmusiccanneedqq.LOGGER.error("Failed to save credential", e);
        }
    }

    public static void clear() {
        credential = null;
        if (credentialFile != null && Files.exists(credentialFile)) {
            try {
                Files.delete(credentialFile);
            } catch (IOException e) {
                Netmusiccanneedqq.LOGGER.error("Failed to delete credential file", e);
            }
        }
    }

    public static QqCredential getCredential() {
        return credential;
    }

    public static boolean hasValidCredential() {
        QqCredential cred = credential;
        return cred != null && cred.isValid();
    }

    public static String getEffectiveCookie() {
        QqCredential cred = credential;
        if (cred != null && cred.isValid()) {
            return cred.toCookieString();
        }
        return "";
    }

    public static String getMusicId() {
        QqCredential cred = credential;
        return cred != null ? cred.getMusicId() : "";
    }
}
