package com.writzx.filtranet;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class CBlock implements Parcelable {
    public static final int MAX_BLOCK_LENGTH = 1024;

    public CBlockType b_type;

    CBlock() {
    }

    protected CBlock(Parcel in) {
        b_type = CBlockType.fromByte(in.readByte());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(b_type.value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

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
