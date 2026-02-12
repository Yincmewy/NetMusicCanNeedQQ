package yincmewy.netmusiccanneedqq.client;

public final class QqSearchState {
    private static boolean opening;

    private QqSearchState() {
    }

    public static boolean isOpening() {
        return opening;
    }

    public static void setOpening(boolean value) {
        opening = value;
    }
}
