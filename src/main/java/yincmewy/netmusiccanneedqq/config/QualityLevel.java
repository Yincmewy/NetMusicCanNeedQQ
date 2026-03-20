package yincmewy.netmusiccanneedqq.config;

public enum QualityLevel {
    LOSSLESS("无损 (FLAC)", 0),
    HIGH("高品质 (MP3)", 1),
    STANDARD("标准 (M4A)", 4);

    private final String label;
    private final int candidateOffset;

    QualityLevel(String label, int candidateOffset) {
        this.label = label;
        this.candidateOffset = candidateOffset;
    }

    public String getLabel() {
        return label;
    }

    public int getCandidateOffset() {
        return candidateOffset;
    }
}
