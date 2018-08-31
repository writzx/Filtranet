package com.writzx.filtranet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

class CUIDBlock extends CBlock {
    short uid;
    int length; // number of bytes occupied by the next field, i.e., UIDs array.
    LinkedList<Short> uids = new LinkedList<>();
    ;
    short next_uid = 0; // uid of the next uid block; same as uid if none (0)

    @Override
    public void read(DataInputStream in) throws IOException {
        uid = in.readShort();
        length = in.readInt();

        byte[] _uids = new byte[length];
        in.read(_uids, 0, length);

        short[] s_uids = new short[length / 2];
        ByteBuffer.wrap(_uids).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(s_uids);

        uids = new LinkedList<>();
        for (short s : s_uids) {
            uids.add(s);
        }

        next_uid = in.readShort();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeShort(uid);
        length = uids.size() * 2;
        out.writeInt(length);

        short[] s_uids = new short[uids.size()];

        for (int i = 0; i < uids.size(); i++) {
            s_uids[i] = uids.get(i);
        }

        byte[] _uids = new byte[length];
        ByteBuffer.wrap(_uids).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(s_uids);

        out.write(_uids, 0, length);

        if (next_uid == 0) next_uid = uid;
        out.write(next_uid);
    }

    CUIDBlock() {
        b_type = CBlockType.UID;
        uid = UID.generate();
    }

    void addUID(short uid) {
        uids.add(uid);
    }

    void insertUID(short uid, int index) {
        uids.add(index, uid);
    }

    void removeUID(int index) {
        uids.remove(index);
    }
}
