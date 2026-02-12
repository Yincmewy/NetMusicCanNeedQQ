package yincmewy.netmusiccanneedqq.data;

import java.util.ArrayList;
import java.util.List;

public final class SongInfoData {
    public String songUrl;
    public String songName;
    public int songTime;
    public String transName = "";
    public boolean vip;
    public boolean readOnly;
    public List<String> artists = new ArrayList<>();

    public boolean isValid() {
        return songUrl != null && !songUrl.isBlank()
                && songName != null && !songName.isBlank()
                && songTime > 0;
    }
}
