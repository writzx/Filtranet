package com.writzx.filtranet;

import com.google.common.primitives.Shorts;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

class CUIDBlock extends CBlock {
    public static final int MAX_UIDS_PER_BLOCK = 500;

    short uid;
    int length; // number of bytes occupied by the next field, i.e., UIDs array.

    List<Short> uids = new ArrayList<>();

    short next_uid = 0; // uid of the next uid block; same as uid if none (0)

    CUIDBlock next;

    @Override
    public void read(DataInputStream in) throws IOException {
        uid = in.readShort();
        length = in.readInt();

        byte[] _uids = new byte[length];
        in.read(_uids, 0, length);

        short[] s_uids = new short[length / 2];
        ByteBuffer.wrap(_uids).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(s_uids);

        uids = Shorts.asList(s_uids);

        next_uid = in.readShort();

        if (next_uid != uid) {
            next = new CUIDBlock(next_uid);
        }

        // save(this, MainActivity.receiveCache);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeShort(uid);
        length = uids.size() * 2;
        out.writeInt(length);

        byte[] _uids = new byte[length];
        ByteBuffer.wrap(_uids).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(Shorts.toArray(uids));

        out.write(_uids, 0, length);

        if (next == null && next_uid == 0) next_uid = uid;
        out.writeShort(next_uid);
    }

    CUIDBlock() {
        b_type = CBlockType.UID;
    }

    CUIDBlock(short uid) {
        b_type = CBlockType.UID;
        this.uid = uid;
        this.next_uid = uid;
    }

    public static void save(CUIDBlock block, String path) throws IOException {
        File file = new File(path + File.separator + block.uid);
        file.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(file, false); DataOutputStream dos = new DataOutputStream(fos)) {
            dos.writeByte(block.b_type.value);
            block.write(dos);
        }
    }

    public static CUIDBlock open(String path, short uid) throws IOException {
        File file = new File(path + File.separator + uid);

        if (!file.exists()) throw new FileNotFoundException();

        try (FileInputStream fin = new FileInputStream(file); DataInputStream din = new DataInputStream(fin)) {
            CBlock blk = CBlock.factory(din);

            if (blk.b_type != CBlockType.UID) {
                throw new UnsupportedOperationException();
            }

            blk.read(din);
            return (CUIDBlock) blk;
        }
    }

    void addUID(short uid) {
        if (uids.size() < MAX_UIDS_PER_BLOCK) {
            uids.add(uid);
        } else {
            if (next == null) {
                next = new CUIDBlock();
                next_uid = next.uid;
            }

            next.addUID(uid);
        }
    }

    void removeUID(short uid) {
        int ind = uids.indexOf(uid);
        if (ind != -1) {
            uids.remove(ind);
        } else {
            if (next != null) {
                next.removeUID(uid);
            }
        }
    }

    void setUid(short uid) {
        this.uid = uid;
        this.next_uid = uid;
    }
}
