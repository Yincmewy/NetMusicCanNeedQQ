package yincmewy.netmusiccanneedqq.qq;

import yincmewy.netmusiccanneedqq.data.SongInfoData;

public final class QqSearchResult {
    private final String id;
    private final String title;
    private final boolean vip;
    private final String singer;

    public QqSearchResult(String id, String title, String singer, boolean isVip) {
        this.id = id == null ? "" : id;
        this.title = title == null ? "" : title;
        this.vip = isVip;
        this.singer = singer == null ? "" : singer;
    }

    public String getTitle() {
        return title;
    }

    public boolean isVip() {
        return vip;
    }

    public String getId() {
        return id;
    }

    public String getDisplayText() {
        if (title.isBlank()) {
            return "空";
        }
        return title + " - " + singer;
    }
}
