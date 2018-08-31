package com.writzx.filtranet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class CMetaBlock extends CBlock {
    int length;
    short attached_uid; // uid of the attached uid block

    CUIDBlock uid_block;

    short nameLength; // not used outside
    short mimeTypeLength; // not used outside

    String filename;
    String mimeType;

    @Override
    public void read(DataInputStream in) throws IOException {
        length = in.readInt();
        attached_uid = in.readShort();

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
        byte[] _mime = filename.getBytes(StandardCharsets.UTF_8);

        nameLength = (short) _name.length;
        mimeTypeLength = (short) _mime.length;

        out.writeInt(length);
        out.writeShort(attached_uid);

        out.writeShort(nameLength);
        out.writeShort(mimeTypeLength);

        out.write(_name, 0, nameLength);
        out.write(_mime, 0, mimeTypeLength);
    }

    CMetaBlock() {
        b_type = CBlockType.Meta;
        uid_block = new CUIDBlock();
    }
}
