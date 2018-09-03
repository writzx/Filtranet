package com.writzx.filtranet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

class CFileBlock extends CBlock {
    short uid;
    long offset;
    short crc;
    int length;

    boolean valid = true;

    CFile cfile;

    @Override
    public void write(DataOutputStream out) throws IOException {
        if (cfile == null || cfile.fd == null || !cfile.fd.valid())
            throw new FileNotFoundException();

        byte[] data = new byte[length];

        try (FileInputStream fis = new FileInputStream(cfile.fd)) {
            fis.skip(offset);
            fis.read(data, 0, length);
        }

        out.writeShort(uid);

        out.writeLong(offset);
        out.writeShort(crc);
        out.writeInt(length);

        out.write(data, 0, length);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        uid = in.readShort();

        offset = in.readLong();
        crc = in.readShort();
        length = in.readInt();

        byte[] data = new byte[length];
        if (in.read(data, 0, length) == -1) {
            throw new EOFException("early end of block");
        }

        if (valid = valid(data)) {
            // the data is valid; write to temp block directory
            File f = new File(MainActivity.receiveCache, "" + uid);
            f.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(f, false); DataOutputStream out = new DataOutputStream(fos)) {
                out.writeShort(uid);

                out.writeLong(offset);
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
