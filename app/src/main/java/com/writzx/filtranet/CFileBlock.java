package com.writzx.filtranet;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CFileBlock extends CBlock implements Parcelable {
    int uid;
    int offset;
    short crc;
    int length;

    boolean valid = true;

    CFile cfile;

    protected CFileBlock(Parcel in) {
        super(in);
        uid = in.readInt();
        offset = in.readInt();
        crc = (short) in.readInt();
        length = in.readInt();
        valid = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(uid);
        dest.writeInt(offset);
        dest.writeInt((int) crc);
        dest.writeInt(length);
        dest.writeByte((byte) (valid ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CFileBlock> CREATOR = new Creator<CFileBlock>() {
        @Override
        public CFileBlock createFromParcel(Parcel in) {
            return new CFileBlock(in);
        }

        @Override
        public CFileBlock[] newArray(int size) {
            return new CFileBlock[size];
        }
    };

    @Override
    public void write(DataOutputStream out) throws IOException {
        if (cfile == null || cfile.fd == null || !cfile.fd.valid())
            throw new FileNotFoundException();

        byte[] data = new byte[length];

        try (FileInputStream fis = new FileInputStream(cfile.fd)) {
            fis.skip(offset);
            fis.read(data, 0, length);
        }

        out.writeInt(uid);

        out.writeInt(offset);
        out.writeShort(crc);
        out.writeInt(length);

        out.write(data, 0, length);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        uid = in.readInt();

        offset = in.readInt();
        crc = in.readShort();
        length = in.readInt();

        byte[] data = new byte[length];
        if (in.read(data, 0, length) == -1) {
            throw new EOFException("early end of block");
        }

        if (valid = valid(data)) {
            // the data is valid; write to temp block directory
            File f = new File(MainActivity.receiveCache, Utils.toHex(uid));
            f.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(f, false); DataOutputStream out = new DataOutputStream(fos)) {
                out.writeInt(uid);

                out.writeInt(offset);
                out.writeShort(crc);
                out.writeLong(length);

                out.write(data, 0, length);
            }
        } // else the data is invalid; ignore the block until requested
    }

    private boolean valid(byte[] data) {
        if (uid == 0 || offset < 0 || length < offset || length != data.length) {
            return false;
        }

        CRC16 crc16 = new CRC16();
        crc16.update(data, 0, data.length);

        return crc16.getShortValue() == crc;
    }

    CFileBlock() {
        b_type = CBlockType.File;
    }
}
