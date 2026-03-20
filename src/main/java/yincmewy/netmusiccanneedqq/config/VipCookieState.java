package yincmewy.netmusiccanneedqq.config;

import yincmewy.netmusiccanneedqq.qq.QqCredentialManager;

public final class VipCookieState {
    private static volatile boolean serverVipCookieAvailable;

    private VipCookieState() {
    }

    public static String getClientEffectiveVipCookie() {
        String cred = QqCredentialManager.getEffectiveCookie();
        if (hasText(cred)) {
            return cred;
        }
        return sanitizeCookie(ClientConfig.getVipCookie());
    }

    public static boolean hasClientEffectiveVipCookie() {
        return hasText(getClientEffectiveVipCookie());
    }

    public static String getServerEffectiveVipCookie() {
        String cred = QqCredentialManager.getEffectiveCookie();
        if (hasText(cred)) {
            return cred;
        }
        return firstNonBlank(ServerConfig.getVipCookie(), ClientConfig.getVipCookie());
    }

    public static boolean hasServerVipCookieAvailable() {
        return serverVipCookieAvailable;
    }

    public static boolean canSkipVipCookieWarningOnClient() {
        return QqCredentialManager.hasValidCredential()
                || hasClientEffectiveVipCookie()
                || hasServerVipCookieAvailable();
    }

    public static void setServerVipCookieAvailable(boolean available) {
        serverVipCookieAvailable = available;
    }

    private static String sanitizeCookie(String cookie) {
        if (cookie == null) {
            return "";
        }
        return cookie.trim();
    }

    private static String firstNonBlank(String first, String second) {
        if (hasText(first)) {
            return first.trim();
        }
        if (hasText(second)) {
            return second.trim();
        }
        return "";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
