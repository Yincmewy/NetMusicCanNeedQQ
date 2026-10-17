package yincmewy.netmusiccanneedqq.qq;

import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;

import java.nio.charset.StandardCharsets;
import java.util.zip.InflaterInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * QQ音乐歌词解密器
 * 使用TripleDES (ECB模式) + zlib解压解密QRC格式歌词
 * 移植自163MusicLyrics项目的QQMusicearchUtils.cs
 */
public final class QqLyricDecrypter {
    // 24字节 TripleDES 密钥
    private static final byte[] QQ_KEY = "!@#)(*$%123ZXC!@!@#)(NHL".getBytes(StandardCharsets.US_ASCII);

    // DES S-Box
    private static final int[] SBOX1 = {
        14,  4, 13,  1,  2, 15, 11,  8,  3, 10,  6, 12,  5,  9,  0,  7,
         0, 15,  7,  4, 14,  2, 13,  1, 10,  6, 12, 11,  9,  5,  3,  8,
         4,  1, 14,  8, 13,  6,  2, 11, 15, 12,  9,  7,  3, 10,  5,  0,
        15, 12,  8,  2,  4,  9,  1,  7,  5, 11,  3, 14, 10,  0,  6, 13
    };

    private static final int[] SBOX2 = {
        15,  1,  8, 14,  6, 11,  3,  4,  9,  7,  2, 13, 12,  0,  5, 10,
         3, 13,  4,  7, 15,  2,  8, 15, 12,  0,  1, 10,  6,  9, 11,  5,
         0, 14,  7, 11, 10,  4, 13,  1,  5,  8, 12,  6,  9,  3,  2, 15,
        13,  8, 10,  1,  3, 15,  4,  2, 11,  6,  7, 12,  0,  5, 14,  9
    };

    private static final int[] SBOX3 = {
        10,  0,  9, 14,  6,  3, 15,  5,  1, 13, 12,  7, 11,  4,  2,  8,
        13,  7,  0,  9,  3,  4,  6, 10,  2,  8,  5, 14, 12, 11, 15,  1,
        13,  6,  4,  9,  8, 15,  3,  0, 11,  1,  2, 12,  5, 10, 14,  7,
         1, 10, 13,  0,  6,  9,  8,  7,  4, 15, 14,  3, 11,  5,  2, 12
    };

    private static final int[] SBOX4 = {
         7, 13, 14,  3,  0,  6,  9, 10,  1,  2,  8,  5, 11, 12,  4, 15,
        13,  8, 11,  5,  6, 15,  0,  3,  4,  7,  2, 12,  1, 10, 14,  9,
        10,  6,  9,  0, 12, 11,  7, 13, 15,  1,  3, 14,  5,  2,  8,  4,
         3, 15,  0,  6, 10, 10, 13,  8,  9,  4,  5, 11, 12,  7,  2, 14
    };

    private static final int[] SBOX5 = {
         2, 12,  4,  1,  7, 10, 11,  6,  8,  5,  3, 15, 13,  0, 14,  9,
        14, 11,  2, 12,  4,  7, 13,  1,  5,  0, 15, 10,  3,  9,  8,  6,
         4,  2,  1, 11, 10, 13,  7,  8, 15,  9, 12,  5,  6,  3,  0, 14,
        11,  8, 12,  7,  1, 14,  2, 13,  6, 15,  0,  9, 10,  4,  5,  3
    };

    private static final int[] SBOX6 = {
        12,  1, 10, 15,  9,  2,  6,  8,  0, 13,  3,  4, 14,  7,  5, 11,
        10, 15,  4,  2,  7, 12,  9,  5,  6,  1, 13, 14,  0, 11,  3,  8,
         9, 14, 15,  5,  2,  8, 12,  3,  7,  0,  4, 10,  1, 13, 11,  6,
         4,  3,  2, 12,  9,  5, 15, 10, 11, 14,  1,  7,  6,  0,  8, 13
    };

    private static final int[] SBOX7 = {
         4, 11,  2, 14, 15,  0,  8, 13,  3, 12,  9,  7,  5, 10,  6,  1,
        13,  0, 11,  7,  4,  9,  1, 10, 14,  3,  5, 12,  2, 15,  8,  6,
         1,  4, 11, 13, 12,  3,  7, 14, 10, 15,  6,  8,  0,  5,  9,  2,
         6, 11, 13,  8,  1,  4, 10,  7,  9,  5,  0, 15, 14,  2,  3, 12
    };

    private static final int[] SBOX8 = {
        13,  2,  8,  4,  6, 15, 11,  1, 10,  9,  3, 14,  5,  0, 12,  7,
         1, 15, 13,  8, 10,  3,  7,  4, 12,  5,  6, 11,  0, 14,  9,  2,
         7, 11,  4,  1,  9, 12, 14,  2,  0,  6, 10, 13, 15,  3,  5,  8,
         2,  1, 14,  7,  4, 10,  8, 13, 15, 12,  9,  0,  3,  5,  6, 11
    };

    private static final int[] KEY_RND_SHIFT = { 1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1 };

    private static final int[] KEY_PERM_C = {
        56, 48, 40, 32, 24, 16,  8,  0, 57, 49, 41, 33, 25, 17,
         9,  1, 58, 50, 42, 34, 26, 18, 10,  2, 59, 51, 43, 35
    };

    private static final int[] KEY_PERM_D = {
        62, 54, 46, 38, 30, 22, 14,  6, 61, 53, 45, 37, 29, 21,
        13,  5, 60, 52, 44, 36, 28, 20, 12,  4, 27, 19, 11,  3
    };

    private static final int[] KEY_COMPRESSION = {
        13, 16, 10, 23,  0,  4,  2, 27, 14,  5, 20,  9,
        22, 18, 11,  3, 25,  7, 15,  6, 26, 19, 12,  1,
        40, 51, 30, 36, 46, 54, 29, 39, 50, 44, 32, 47,
        43, 48, 38, 55, 33, 52, 45, 41, 49, 35, 28, 31
    };

    private static final long ENCRYPT = 1;
    private static final long DECRYPT = 0;

    private QqLyricDecrypter() {
    }

    // ==================== Bit manipulation helpers ====================

    private static long bitnum(byte[] a, int b, int c) {
        return ((long)((a[b / 32 * 4 + 3 - b % 32 / 8] >> (7 - b % 8)) & 0x01)) << c;
    }

    private static byte bitnumIntr(long a, int b, int c) {
        return (byte)(((a >> (31 - b)) & 0x00000001L) << c);
    }

    private static long bitnumIntl(long a, int b, int c) {
        return ((a << b) & 0x80000000L) >>> c;
    }

    private static long sboxbit(byte a) {
        return ((long)(a & 0x20)) | (((long)(a & 0x1f)) >> 1) | (((long)(a & 0x01)) << 4);
    }

    // ==================== DES core ====================

    private static void keySchedule(byte[] key, byte[][] schedule, long mode) {
        long C = 0, D = 0;

        for (int i = 0, j = 31; i < 28; ++i, --j) {
            C |= bitnum(key, KEY_PERM_C[i], j);
        }
        for (int i = 0, j = 31; i < 28; ++i, --j) {
            D |= bitnum(key, KEY_PERM_D[i], j);
        }

        for (int i = 0; i < 16; ++i) {
            C = ((C << KEY_RND_SHIFT[i]) | (C >>> (28 - KEY_RND_SHIFT[i]))) & 0xfffffff0L;
            D = ((D << KEY_RND_SHIFT[i]) | (D >>> (28 - KEY_RND_SHIFT[i]))) & 0xfffffff0L;

            int toGen = (mode == DECRYPT) ? (15 - i) : i;

            for (int j = 0; j < 6; ++j) {
                schedule[toGen][j] = 0;
            }

            int j = 0;
            for (; j < 24; ++j) {
                schedule[toGen][j / 8] |= bitnumIntr(C, KEY_COMPRESSION[j], 7 - (j % 8));
            }
            for (; j < 48; ++j) {
                schedule[toGen][j / 8] |= bitnumIntr(D, KEY_COMPRESSION[j] - 27, 7 - (j % 8));
            }
        }
    }

    private static void ip(long[] state, byte[] input) {
        state[0] = bitnum(input, 57, 31) | bitnum(input, 49, 30) | bitnum(input, 41, 29) | bitnum(input, 33, 28) |
                   bitnum(input, 25, 27) | bitnum(input, 17, 26) | bitnum(input,  9, 25) | bitnum(input,  1, 24) |
                   bitnum(input, 59, 23) | bitnum(input, 51, 22) | bitnum(input, 43, 21) | bitnum(input, 35, 20) |
                   bitnum(input, 27, 19) | bitnum(input, 19, 18) | bitnum(input, 11, 17) | bitnum(input,  3, 16) |
                   bitnum(input, 61, 15) | bitnum(input, 53, 14) | bitnum(input, 45, 13) | bitnum(input, 37, 12) |
                   bitnum(input, 29, 11) | bitnum(input, 21, 10) | bitnum(input, 13,  9) | bitnum(input,  5,  8) |
                   bitnum(input, 63,  7) | bitnum(input, 55,  6) | bitnum(input, 47,  5) | bitnum(input, 39,  4) |
                   bitnum(input, 31,  3) | bitnum(input, 23,  2) | bitnum(input, 15,  1) | bitnum(input,  7,  0);

        state[1] = bitnum(input, 56, 31) | bitnum(input, 48, 30) | bitnum(input, 40, 29) | bitnum(input, 32, 28) |
                   bitnum(input, 24, 27) | bitnum(input, 16, 26) | bitnum(input,  8, 25) | bitnum(input,  0, 24) |
                   bitnum(input, 58, 23) | bitnum(input, 50, 22) | bitnum(input, 42, 21) | bitnum(input, 34, 20) |
                   bitnum(input, 26, 19) | bitnum(input, 18, 18) | bitnum(input, 10, 17) | bitnum(input,  2, 16) |
                   bitnum(input, 60, 15) | bitnum(input, 52, 14) | bitnum(input, 44, 13) | bitnum(input, 36, 12) |
                   bitnum(input, 28, 11) | bitnum(input, 20, 10) | bitnum(input, 12,  9) | bitnum(input,  4,  8) |
                   bitnum(input, 62,  7) | bitnum(input, 54,  6) | bitnum(input, 46,  5) | bitnum(input, 38,  4) |
                   bitnum(input, 30,  3) | bitnum(input, 22,  2) | bitnum(input, 14,  1) | bitnum(input,  6,  0);
    }

    private static void invIp(long[] state, byte[] input) {
        input[3] = (byte)(bitnumIntr(state[1],  7, 7) | bitnumIntr(state[0],  7, 6) | bitnumIntr(state[1], 15, 5) |
                          bitnumIntr(state[0], 15, 4) | bitnumIntr(state[1], 23, 3) | bitnumIntr(state[0], 23, 2) |
                          bitnumIntr(state[1], 31, 1) | bitnumIntr(state[0], 31, 0));

        input[2] = (byte)(bitnumIntr(state[1],  6, 7) | bitnumIntr(state[0],  6, 6) | bitnumIntr(state[1], 14, 5) |
                          bitnumIntr(state[0], 14, 4) | bitnumIntr(state[1], 22, 3) | bitnumIntr(state[0], 22, 2) |
                          bitnumIntr(state[1], 30, 1) | bitnumIntr(state[0], 30, 0));

        input[1] = (byte)(bitnumIntr(state[1],  5, 7) | bitnumIntr(state[0],  5, 6) | bitnumIntr(state[1], 13, 5) |
                          bitnumIntr(state[0], 13, 4) | bitnumIntr(state[1], 21, 3) | bitnumIntr(state[0], 21, 2) |
                          bitnumIntr(state[1], 29, 1) | bitnumIntr(state[0], 29, 0));

        input[0] = (byte)(bitnumIntr(state[1],  4, 7) | bitnumIntr(state[0],  4, 6) | bitnumIntr(state[1], 12, 5) |
                          bitnumIntr(state[0], 12, 4) | bitnumIntr(state[1], 20, 3) | bitnumIntr(state[0], 20, 2) |
                          bitnumIntr(state[1], 28, 1) | bitnumIntr(state[0], 28, 0));

        input[7] = (byte)(bitnumIntr(state[1],  3, 7) | bitnumIntr(state[0],  3, 6) | bitnumIntr(state[1], 11, 5) |
                          bitnumIntr(state[0], 11, 4) | bitnumIntr(state[1], 19, 3) | bitnumIntr(state[0], 19, 2) |
                          bitnumIntr(state[1], 27, 1) | bitnumIntr(state[0], 27, 0));

        input[6] = (byte)(bitnumIntr(state[1],  2, 7) | bitnumIntr(state[0],  2, 6) | bitnumIntr(state[1], 10, 5) |
                          bitnumIntr(state[0], 10, 4) | bitnumIntr(state[1], 18, 3) | bitnumIntr(state[0], 18, 2) |
                          bitnumIntr(state[1], 26, 1) | bitnumIntr(state[0], 26, 0));

        input[5] = (byte)(bitnumIntr(state[1],  1, 7) | bitnumIntr(state[0],  1, 6) | bitnumIntr(state[1],  9, 5) |
                          bitnumIntr(state[0],  9, 4) | bitnumIntr(state[1], 17, 3) | bitnumIntr(state[0], 17, 2) |
                          bitnumIntr(state[1], 25, 1) | bitnumIntr(state[0], 25, 0));

        input[4] = (byte)(bitnumIntr(state[1],  0, 7) | bitnumIntr(state[0],  0, 6) | bitnumIntr(state[1],  8, 5) |
                          bitnumIntr(state[0],  8, 4) | bitnumIntr(state[1], 16, 3) | bitnumIntr(state[0], 16, 2) |
                          bitnumIntr(state[1], 24, 1) | bitnumIntr(state[0], 24, 0));
    }

    private static long f(long state, byte[] key) {
        byte[] lrgstate = new byte[6];

        long t1 = bitnumIntl(state, 31, 0) | ((state & 0xf0000000L) >>> 1) | bitnumIntl(state, 4, 5) |
                   bitnumIntl(state, 3, 6) | ((state & 0x0f000000L) >>> 3) | bitnumIntl(state, 8, 11) |
                   bitnumIntl(state, 7, 12) | ((state & 0x00f00000L) >>> 5) | bitnumIntl(state, 12, 17) |
                   bitnumIntl(state, 11, 18) | ((state & 0x000f0000L) >>> 7) | bitnumIntl(state, 16, 23);

        long t2 = bitnumIntl(state, 15, 0) | ((state & 0x0000f000L) << 15) | bitnumIntl(state, 20, 5) |
                   bitnumIntl(state, 19, 6) | ((state & 0x00000f00L) << 13) | bitnumIntl(state, 24, 11) |
                   bitnumIntl(state, 23, 12) | ((state & 0x000000f0L) << 11) | bitnumIntl(state, 28, 17) |
                   bitnumIntl(state, 27, 18) | ((state & 0x0000000fL) << 9) | bitnumIntl(state, 0, 23);

        lrgstate[0] = (byte)((t1 >>> 24) & 0xff);
        lrgstate[1] = (byte)((t1 >>> 16) & 0xff);
        lrgstate[2] = (byte)((t1 >>> 8) & 0xff);
        lrgstate[3] = (byte)((t2 >>> 24) & 0xff);
        lrgstate[4] = (byte)((t2 >>> 16) & 0xff);
        lrgstate[5] = (byte)((t2 >>> 8) & 0xff);

        for (int i = 0; i < 6; i++) {
            lrgstate[i] ^= key[i];
        }

        // 注意：Java的byte是有符号的，右移会符号扩展。需要用 & 0xFF 转为无符号后再移位
        state = ((long)SBOX1[(int)sboxbit((byte)((lrgstate[0] & 0xFF) >> 2))]) << 28 |
                ((long)SBOX2[(int)sboxbit((byte)(((lrgstate[0] & 0x03) << 4) | ((lrgstate[1] & 0xFF) >> 4)))]) << 24 |
                ((long)SBOX3[(int)sboxbit((byte)(((lrgstate[1] & 0x0f) << 2) | ((lrgstate[2] & 0xFF) >> 6)))]) << 20 |
                ((long)SBOX4[(int)sboxbit((byte)(lrgstate[2] & 0x3f))]) << 16 |
                ((long)SBOX5[(int)sboxbit((byte)((lrgstate[3] & 0xFF) >> 2))]) << 12 |
                ((long)SBOX6[(int)sboxbit((byte)(((lrgstate[3] & 0x03) << 4) | ((lrgstate[4] & 0xFF) >> 4)))]) << 8 |
                ((long)SBOX7[(int)sboxbit((byte)(((lrgstate[4] & 0x0f) << 2) | ((lrgstate[5] & 0xFF) >> 6)))]) << 4 |
                (long)SBOX8[(int)sboxbit((byte)(lrgstate[5] & 0x3f))];

        state = bitnumIntl(state, 15, 0) | bitnumIntl(state,  6,  1) | bitnumIntl(state, 19,  2) |
                bitnumIntl(state, 20, 3) | bitnumIntl(state, 28,  4) | bitnumIntl(state, 11,  5) |
                bitnumIntl(state, 27, 6) | bitnumIntl(state, 16,  7) | bitnumIntl(state,  0,  8) |
                bitnumIntl(state, 14, 9) | bitnumIntl(state, 22, 10) | bitnumIntl(state, 25, 11) |
                bitnumIntl(state,  4,12) | bitnumIntl(state, 17, 13) | bitnumIntl(state, 30, 14) |
                bitnumIntl(state,  9,15) | bitnumIntl(state,  1, 16) | bitnumIntl(state,  7, 17) |
                bitnumIntl(state, 23,18) | bitnumIntl(state, 13, 19) | bitnumIntl(state, 31, 20) |
                bitnumIntl(state, 26,21) | bitnumIntl(state,  2, 22) | bitnumIntl(state,  8, 23) |
                bitnumIntl(state, 18,24) | bitnumIntl(state, 12, 25) | bitnumIntl(state, 29, 26) |
                bitnumIntl(state,  5,27) | bitnumIntl(state, 21, 28) | bitnumIntl(state, 10, 29) |
                bitnumIntl(state,  3,30) | bitnumIntl(state, 24, 31);

        return state;
    }

    private static void desCrypt(byte[] input, byte[] output, byte[][] key) {
        long[] state = new long[2];
        ip(state, input);

        for (int idx = 0; idx < 15; ++idx) {
            long t = state[1];
            state[1] = f(state[1], key[idx]) ^ state[0];
            state[0] = t;
        }

        state[0] = f(state[1], key[15]) ^ state[0];
        invIp(state, output);
    }

    private static void tripleDESKeySetup(byte[] key, byte[][][] schedule, long mode) {
        byte[] key0 = new byte[8];
        byte[] key1 = new byte[8];
        byte[] key2 = new byte[8];
        System.arraycopy(key, 0, key0, 0, 8);
        System.arraycopy(key, 8, key1, 0, 8);
        System.arraycopy(key, 16, key2, 0, 8);

        if (mode == ENCRYPT) {
            keySchedule(key0, schedule[0], ENCRYPT);
            keySchedule(key1, schedule[1], DECRYPT);
            keySchedule(key2, schedule[2], ENCRYPT);
        } else {
            keySchedule(key0, schedule[2], DECRYPT);
            keySchedule(key1, schedule[1], ENCRYPT);
            keySchedule(key2, schedule[0], DECRYPT);
        }
    }

    private static void tripleDESCrypt(byte[] input, byte[] output, byte[][][] key) {
        byte[] temp = new byte[8];
        desCrypt(input, temp, key[0]);
        desCrypt(temp, output, key[1]);
        desCrypt(output, output, key[2]);
    }

    // ==================== Public API ====================

    /**
     * 检查字符串是否为合法的十六进制格式
     */
    public static boolean isHex(String s) {
        if (s == null || s.isEmpty() || (s.length() % 2) != 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解密QQ音乐QRC歌词
     *
     * @param encryptedHex 十六进制加密歌词字符串
     * @return 解密后的明文歌词，失败返回null
     */
    public static String decryptLyrics(String encryptedHex) {
        try {
            byte[] encryptedBytes = hexStringToByteArray(encryptedHex);
            byte[] data = new byte[encryptedBytes.length];

            // 初始化3DES密钥调度
            byte[][][] schedule = new byte[3][16][6];
            tripleDESKeySetup(QQ_KEY, schedule, DECRYPT);

            // 按8字节分组解密
            for (int i = 0; i < encryptedBytes.length; i += 8) {
                byte[] block = new byte[8];
                System.arraycopy(encryptedBytes, i, block, 0, Math.min(8, encryptedBytes.length - i));
                byte[] temp = new byte[8];
                tripleDESCrypt(block, temp, schedule);
                System.arraycopy(temp, 0, data, i, Math.min(8, encryptedBytes.length - i));
            }

            // 输出解密后的前几个字节用于调试
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(16, data.length); i++) {
                sb.append(String.format("%02X ", data[i] & 0xFF));
            }
            Netmusiccanneedqq.LOGGER.info("Decrypted bytes (first 16): {}", sb.toString());

            // zlib解压
            byte[] decompressed = zlibDecompress(data);
            return new String(decompressed, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            Netmusiccanneedqq.LOGGER.error("Failed to decrypt QQ lyrics: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 十六进制字符串转字节数组
     */
    private static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte)((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }

    /**
     * zlib解压
     * 使用手动控制Inflater的方式，可以获取部分解压的数据
     * 先尝试标准zlib格式，失败则尝试raw deflate格式
     */
    private static byte[] zlibDecompress(byte[] data) {
        // 先尝试标准zlib格式
        byte[] result = inflateRaw(data, false);
        if (result != null && result.length > 0) {
            return result;
        }
        // 标准zlib失败，尝试raw deflate格式
        result = inflateRaw(data, true);
        if (result != null && result.length > 0) {
            return result;
        }
        // 两种格式都失败
        return new byte[0];
    }

    /**
     * 使用手动控制Inflater进行解压，即使遇到错误也能返回已解压的部分数据
     */
    private static byte[] inflateRaw(byte[] data, boolean rawDeflate) {
        java.util.zip.Inflater inflater = new java.util.zip.Inflater(rawDeflate);
        try {
            inflater.setInput(data);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            while (!inflater.finished() && !inflater.needsInput()) {
                try {
                    int count = inflater.inflate(buffer);
                    if (count > 0) {
                        baos.write(buffer, 0, count);
                    } else if (inflater.needsDictionary()) {
                        break;
                    }
                } catch (java.util.zip.DataFormatException e) {
                    // 解压出错，但返回已解压的部分数据
                    Netmusiccanneedqq.LOGGER.debug("inflate error (raw={}): {}, partial data: {} bytes",
                            rawDeflate, e.getMessage(), baos.size());
                    break;
                }
            }
            return baos.toByteArray();
        } finally {
            inflater.end();
        }
    }
}
