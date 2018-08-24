package com.writzx.filtranet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class CUIDBlock extends CBlock {
    short uid;
    int length; // number of bytes occupied by the next field, i.e., UIDs array.
    short[] uids;
    short next_uid = 0; // uid of the next uid block; same as uid if none (0)

    @Override
    public void read(DataInputStream in) throws IOException {
        uid = in.readShort();
        length = in.readInt();

        byte[] _uids = new byte[length];
        in.read(_uids, 0, length);

        uids = new short[length / 2];
        ByteBuffer.wrap(_uids).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(uids);

        next_uid = in.readShort();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeShort(uid);
        length = uids.length * 2;
        out.writeInt(length);

        byte[] _uids = new byte[length];
        ByteBuffer.wrap(_uids).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(uids);

        out.write(_uids, 0, length);

        if (next_uid == 0) next_uid = uid;
        out.write(next_uid);
    }

    CUIDBlock() {
        b_type = CBlockType.UID;
    }
}
