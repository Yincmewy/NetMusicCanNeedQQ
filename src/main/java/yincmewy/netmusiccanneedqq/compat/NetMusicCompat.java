package yincmewy.netmusiccanneedqq.compat;

import org.slf4j.Logger;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.data.SongInfoData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class NetMusicCompat {
    private static final Logger LOGGER = Netmusiccanneedqq.LOGGER;
    private static final String SONG_INFO_CLASS = "com.github.tartaricacid.netmusic.item.ItemMusicCD$SongInfo";
    private static final String SET_MESSAGE_CLASS = "com.github.tartaricacid.netmusic.network.message.SetMusicIDMessage";
    private static final String NETWORK_HANDLER_CLASS = "com.github.tartaricacid.netmusic.network.NetworkHandler";

    private static boolean initialized;
    private static boolean initFailed;
    private static Constructor<?> songInfoCtor;
    private static Constructor<?> setMessageCtor;
    private static Method sendToServerMethod;
    private static Object channel;
    private static Field transNameField;
    private static Field vipField;
    private static Field readOnlyField;
    private static Field artistsField;

    private NetMusicCompat() {
    }

    public static boolean sendSongToServer(SongInfoData info) {
        if (info == null || !info.isValid()) {
            return false;
        }
        if (!init()) {
            return false;
        }
        try {
            Object songInfo = songInfoCtor.newInstance(info.songUrl, info.songName, info.songTime, info.readOnly);
            applyOptionalFields(songInfo, info);
            Object message = setMessageCtor.newInstance(songInfo);
            if (channel != null) {
                sendToServerMethod.invoke(channel, message);
            } else {
                sendToServerMethod.invoke(null, message);
            }
            return true;
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Failed to send NetMusic song info message", e);
            return false;
        }
    }

    private static synchronized boolean init() {
        if (initialized) {
            return !initFailed;
        }
        initialized = true;
        try {
            Class<?> songInfoClass = Class.forName(SONG_INFO_CLASS);
            songInfoCtor = songInfoClass.getConstructor(String.class, String.class, int.class, boolean.class);
            setMessageCtor = Class.forName(SET_MESSAGE_CLASS).getConstructor(songInfoClass);
            Class<?> networkHandlerClass = Class.forName(NETWORK_HANDLER_CLASS);
            try {
                Field channelField = networkHandlerClass.getField("CHANNEL");
                channel = channelField.get(null);
                sendToServerMethod = channel.getClass().getMethod("sendToServer", Object.class);
            } catch (NoSuchFieldException ignored) {
                Class<?> payloadClass = Class.forName("net.minecraft.network.protocol.common.custom.CustomPacketPayload");
                sendToServerMethod = networkHandlerClass.getMethod("sendToServer", payloadClass);
                channel = null;
            }

            transNameField = songInfoClass.getField("transName");
            vipField = songInfoClass.getField("vip");
            readOnlyField = songInfoClass.getField("readOnly");
            artistsField = songInfoClass.getField("artists");
        } catch (ReflectiveOperationException e) {
            initFailed = true;
            LOGGER.error("NetMusic classes not found, QQ compatibility disabled", e);
        }
        return !initFailed;
    }

    private static void applyOptionalFields(Object songInfo, SongInfoData info) throws IllegalAccessException {
        if (transNameField != null && info.transName != null && !info.transName.isBlank()) {
            transNameField.set(songInfo, info.transName);
        }
        if (vipField != null) {
            vipField.setBoolean(songInfo, info.vip);
        }
        if (readOnlyField != null) {
            readOnlyField.setBoolean(songInfo, info.readOnly);
        }
        if (artistsField != null && info.artists != null && !info.artists.isEmpty()) {
            List<String> copy = new ArrayList<>(info.artists);
            artistsField.set(songInfo, copy);
        }
    }
}
