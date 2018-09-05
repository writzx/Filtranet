package com.writzx.filtranet;

import java.util.Random;

public class UID {
    private static final Random random = new Random();

    public static int generate() {
        return random.nextInt();
    }
}
