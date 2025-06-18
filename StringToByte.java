package com.example.ldapdemo.util;

public class SidUtils {

    public static byte[] sidStringToBytes(String sidString) {
        String[] parts = sidString.split("-");
        byte revision = (byte) Integer.parseInt(parts[1]);
        byte subAuthCount = (byte) (parts.length - 3);
        byte[] sid = new byte[8 + 4 * subAuthCount];

        sid[0] = revision;
        sid[1] = subAuthCount;

        long authority = Long.parseLong(parts[2]);
        sid[2] = (byte) ((authority >> 40) & 0xFF);
        sid[3] = (byte) ((authority >> 32) & 0xFF);
        sid[4] = (byte) ((authority >> 24) & 0xFF);
        sid[5] = (byte) ((authority >> 16) & 0xFF);
        sid[6] = (byte) ((authority >> 8) & 0xFF);
        sid[7] = (byte) (authority & 0xFF);

        for (int i = 0; i < subAuthCount; i++) {
            long subAuthority = Long.parseLong(parts[i + 3]);
            sid[8 + i * 4] = (byte) (subAuthority & 0xFF);
            sid[8 + i * 4 + 1] = (byte) ((subAuthority >> 8) & 0xFF);
            sid[8 + i * 4 + 2] = (byte) ((subAuthority >> 16) & 0xFF);
            sid[8 + i * 4 + 3] = (byte) ((subAuthority >> 24) & 0xFF);
        }

        return sid;
    }
}
