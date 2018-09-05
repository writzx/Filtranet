package com.writzx.filtranet;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.primitives.Ints;

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

class CUIDBlock extends CBlock implements Parcelable {
    public static final int MAX_UIDS_PER_BLOCK = 256;

    int uid;
    int length; // number of bytes occupied by the next field, i.e., UIDs array.

    List<Integer> uids = new ArrayList<>();

    int next_uid = 0; // uid of the next uid block; same as uid if none (0)

    CUIDBlock next;

    protected CUIDBlock(Parcel in) {
        super(in);
        uid = in.readInt();
        length = in.readInt();
        in.readList(uids, Integer.class.getClassLoader());
        next_uid = in.readInt();
        next = in.readParcelable(CUIDBlock.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(uid);
        dest.writeInt(length);
        dest.writeList(uids);
        dest.writeInt(next_uid);
        dest.writeParcelable(next, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CUIDBlock> CREATOR = new Creator<CUIDBlock>() {
        @Override
        public CUIDBlock createFromParcel(Parcel in) {
            return new CUIDBlock(in);
        }

        @Override
        public CUIDBlock[] newArray(int size) {
            return new CUIDBlock[size];
        }
    };

    @Override
    public void read(DataInputStream in) throws IOException {
        uid = in.readInt();
        length = in.readInt();

        byte[] _uids = new byte[length];
        in.read(_uids, 0, length);

        int[] s_uids = new int[length / 4];
        ByteBuffer.wrap(_uids).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(s_uids);

        uids = Ints.asList(s_uids);

        next_uid = in.readInt();

        if (next_uid != uid) {
            next = new CUIDBlock(next_uid);
        }

        // save(this, MainActivity.receiveCache);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(uid);
        length = uids.size() * 4;
        out.writeInt(length);

        byte[] _uids = new byte[length];
        ByteBuffer.wrap(_uids).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(Ints.toArray(uids));

        out.write(_uids, 0, length);

        if (next == null && next_uid == 0) next_uid = uid;
        out.writeInt(next_uid);
    }

    CUIDBlock() {
        this(UID.generate());
    }

    CUIDBlock(int uid) {
        b_type = CBlockType.UID;
        this.uid = uid;
        this.next_uid = uid;
    }

    public static void save(CUIDBlock block, String path) throws IOException {
        File file = new File(path, Utils.toHex(block.uid));
        file.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(file, false); DataOutputStream dos = new DataOutputStream(fos)) {
            dos.writeByte(block.b_type.value);
            block.write(dos);
        }
    }

    public static CUIDBlock open(String path, int uid) throws IOException {
        File file = new File(path, Utils.toHex(uid));

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

    void addUID(int uid) {
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

    void removeUID(int uid) {
        if (!uids.remove(Integer.valueOf(uid)) && next != null) {
            next.removeUID(uid);
        }
    }

    void setUid(int uid) {
        this.uid = uid;
    }

    void setNextUid(int uid) {
        this.next_uid = uid;
    }
}
