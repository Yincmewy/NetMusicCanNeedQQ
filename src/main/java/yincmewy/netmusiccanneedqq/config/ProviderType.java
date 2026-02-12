package yincmewy.netmusiccanneedqq.config;

public enum ProviderType {
    NETEASE("163"),
    QQ("QQ");

    private final String shortLabel;

    ProviderType(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    public ProviderType next() {
        return this == NETEASE ? QQ : NETEASE;
    }

    public String getShortLabel() {
        return shortLabel;
    }
}
