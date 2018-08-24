package com.writzx.filtranet;

public enum CBlockType {
    File(0),
    Info(1),
    Meta(2),
    UID(3);

    byte value;

    CBlockType(int i) {
        value = (byte) i;
    }

    static CBlockType fromByte(byte i) {
        for (CBlockType b : values()) {
            if (b.value == i) return b;
        }
        return File;
    }
}
