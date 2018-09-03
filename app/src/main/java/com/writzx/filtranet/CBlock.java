package com.writzx.filtranet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class CBlock {
    public static final int MAX_BLOCK_LENGTH = 1024;

    public CBlockType b_type;

    abstract void read(DataInputStream in) throws IOException;

    abstract void write(DataOutputStream out) throws IOException;

    public static CBlock factory(DataInputStream in) throws IOException {
        CBlockType b_type = CBlockType.fromByte(in.readByte());

        switch (b_type) {
            case UID:
                return new CUIDBlock();
            case Info:
                return new CInfoBlock();
            case Meta:
                return new CMetaBlock();
            case File:
            default:
                return new CFileBlock();
        }
    }
}
