package org.guardtime.ksi.hlf.wrapper;

public class Version {
    public static final long VER_INVALID = -1;
    public static final long VER_1 = 1;

    public static String getSupportedVersionsString() {
        return "[" + 
                 VER_1 + 
                "]"; 
    }

    public static boolean isSupported(long ver) {
        return ver == VER_1;
    }
}