package com.writzx.filtranet;

import java.util.Random;

public class UID {
    private static final Random random = new Random();

    public static short generate() {
        return (short) random.nextInt(Short.MAX_VALUE + 1);
    }
}
