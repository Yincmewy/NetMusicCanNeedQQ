package yincmewy.netmusiccanneedqq.data;

public final class ParsedUrl {
    private final ResourceType type;
    private final String id;

    public ParsedUrl(ResourceType type, String id) {
        this.type = type;
        this.id = id;
    }

    public ResourceType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public enum ResourceType {
        ALBUM, SONG, PLAYLIST
    }
}
