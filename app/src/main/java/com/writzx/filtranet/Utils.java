package com.writzx.filtranet;

public class Utils {
    public static String toHex(int num) {
        return String.format("%8S", Integer.toHexString(num)).replace(" ", "0");
    }

    public static String toHex(short num) {
        return String.format("%4S", Integer.toHexString(num)).replace(" ", "0");
    }

    public static String toHex(byte num) {
        return String.format("%2S", Integer.toHexString(num)).replace(" ", "0");
    }

    public static String toHex(long num) {
        return String.format("%16S", Long.toHexString(num)).replace(" ", "0");
    }

    public static <T extends Number> T fromHex(String num, Class<T> clz) {
        long val = Long.parseLong(num, 16);
        if (clz == Byte.class) {
            return clz.cast(val & 0xff);
        } else if (clz == Short.class) {
            return clz.cast(val & 0xffff);
        } else {
            return clz.cast(val);
        }
    }
}
