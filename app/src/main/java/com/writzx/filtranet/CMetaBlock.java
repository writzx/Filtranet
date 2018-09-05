package com.writzx.filtranet;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class CMetaBlock extends CBlock implements Parcelable {
    int length;
    int attached_uid; // uid of the attached uid block

    CUIDBlock uid_block;

    short nameLength; // not used outside
    short mimeTypeLength; // not used outside

    String filename;
    String mimeType;

    protected CMetaBlock(Parcel in) {
        super(in);
        length = in.readInt();
        attached_uid = in.readInt();
        uid_block = in.readParcelable(CUIDBlock.class.getClassLoader());
        nameLength = (short) in.readInt();
        mimeTypeLength = (short) in.readInt();
        filename = in.readString();
        mimeType = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(length);
        dest.writeInt(attached_uid);
        dest.writeParcelable(uid_block, flags);
        dest.writeInt((int) nameLength);
        dest.writeInt((int) mimeTypeLength);
        dest.writeString(filename);
        dest.writeString(mimeType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CMetaBlock> CREATOR = new Creator<CMetaBlock>() {
        @Override
        public CMetaBlock createFromParcel(Parcel in) {
            return new CMetaBlock(in);
        }

        @Override
        public CMetaBlock[] newArray(int size) {
            return new CMetaBlock[size];
        }
    };

    @Override
    public void read(DataInputStream in) throws IOException {
        length = in.readInt();
        attached_uid = in.readInt();

        nameLength = in.readShort();
        mimeTypeLength = in.readShort();

        byte[] _name = new byte[nameLength];
        byte[] _mime = new byte[mimeTypeLength];

        in.read(_name, 0, nameLength);
        in.read(_mime, 0, mimeTypeLength);

        filename = new String(_name, StandardCharsets.UTF_8);
        mimeType = new String(_mime, StandardCharsets.UTF_8);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        byte[] _name = filename.getBytes(StandardCharsets.UTF_8);
        byte[] _mime = mimeType.getBytes(StandardCharsets.UTF_8);

        nameLength = (short) _name.length;
        mimeTypeLength = (short) _mime.length;

        out.writeInt(length);
        out.writeInt(attached_uid);

        out.writeShort(nameLength);
        out.writeShort(mimeTypeLength);

        out.write(_name, 0, nameLength);
        out.write(_mime, 0, mimeTypeLength);
    }

    CMetaBlock() {
        b_type = CBlockType.Meta;
        uid_block = new CUIDBlock();
        attached_uid = uid_block.uid;
    }
}
