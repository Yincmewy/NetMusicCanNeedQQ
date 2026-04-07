package yincmewy.netmusiccanneedqq.config;

public enum QualityLevel {
    LOSSLESS("Lossless (FLAC)", 0),
    HIGH("High (MP3)", 1),
    STANDARD("Standard (M4A)", 4);

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
