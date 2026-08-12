package yincmewy.netmusiccanneedqq.qq;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QQ音乐QRC逐字歌词格式转标准LRC格式
 * 移植自163MusicLyrics项目的VerbatimLyricUtils.cs
 *
 * QRC格式示例: [70,80](404,202)你(608,210)好(820,192)世(1014,318)界
 * - [70,80] 行级时间戳: 起始时间70ms, 持续时间80ms
 * - (404,202) 逐字时间戳: 起始时间404ms, 持续时间202ms
 *
 * 转换为标准LRC: [00:00.07]你好世界
 *
 * 同时处理已经是标准LRC格式的内容（直接通过）
 */
public final class QrcToLrcConverter {

    // 匹配QRC行级时间戳 [x,y]（纯数字）
    private static final Pattern VERBATIM_PREFIX_PATTERN = Pattern.compile("^\\[(\\d+),(\\d+)]");
    // 匹配逐字时间戳 (a,b)
    private static final Pattern VERBATIM_TIME_PATTERN = Pattern.compile("\\(\\d+,\\d+\\)");
    // 匹配标准LRC时间戳 [mm:ss.xx] 或 [mm:ss.xxx]
    private static final Pattern STANDARD_LRC_PATTERN = Pattern.compile("^\\[\\d{1,2}:\\d{2}[.:]\\d{2,3}]");
    // 匹配LRC元数据标签 [ti:xxx], [ar:xxx], [al:xxx], [by:xxx], [offset:xxx]
    private static final Pattern LRC_META_PATTERN = Pattern.compile("^\\[(ti|ar|al|by|offset):.*?]", Pattern.CASE_INSENSITIVE);

    private QrcToLrcConverter() {
    }

    /**
     * 将QRC格式歌词转换为标准LRC格式
     * 自动检测输入格式：
     * - 如果是QRC格式（包含[x,y]时间戳），转换为LRC
     * - 如果已经是标准LRC格式，直接返回
     *
     * @param content 歌词内容
     * @return 标准LRC格式歌词
     */
    public static String convertToLrc(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        String[] lines = content.split("\\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            // 跳过LRC元数据标签（如 [ti:xxx], [ar:xxx], [al:xxx] 等）
            if (LRC_META_PATTERN.matcher(line).find()) {
                continue;
            }

            // 检测是否为QRC格式（行首是 [数字,数字]）
            Matcher prefixMatcher = VERBATIM_PREFIX_PATTERN.matcher(line);
            if (prefixMatcher.find()) {
                // QRC格式：提取行起始时间 [x,y] 中的 x（毫秒）
                String timeStr = prefixMatcher.group(1);
                long timestampMs = Long.parseLong(timeStr);

                // 去除行级时间戳后的部分
                String afterPrefix = line.substring(prefixMatcher.end());

                // 去除所有逐字时间戳 (a,b)
                String lyricText = VERBATIM_TIME_PATTERN.matcher(afterPrefix).replaceAll("");

                // 转换为LRC格式 [mm:ss.xx]
                if (!lyricText.isBlank()) {
                    sb.append(formatLrcTimestamp(timestampMs)).append(lyricText);
                }
            } else if (STANDARD_LRC_PATTERN.matcher(line).find()) {
                // 已经是标准LRC格式，直接保留
                sb.append(line);
            } else {
                // 既不是QRC也不是标准LRC（可能是纯文本歌词行），跳过
                continue;
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 将毫秒转换为LRC时间戳格式 [mm:ss.xx]
     */
    private static String formatLrcTimestamp(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long centiseconds = (ms % 1000) / 10;

        return String.format("[%02d:%02d.%02d]", minutes, seconds, centiseconds);
    }
}
