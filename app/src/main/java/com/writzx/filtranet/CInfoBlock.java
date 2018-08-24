package com.writzx.filtranet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class CInfoBlock extends CBlock {
    short attached_uid; // uid of the attached block
    int info_code;
    int messageLength; // not to be used outside of this class
    String message;

    @Override
    public void read(DataInputStream in) throws IOException {
        attached_uid = in.readShort();

        info_code = in.readInt();
        messageLength = in.readInt();

        byte[] _msg = new byte[messageLength];
        in.read(_msg, 0, messageLength);

        message = new String(_msg, StandardCharsets.UTF_8);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeShort(attached_uid);

        out.writeInt(info_code);

        byte[] _msg = message.getBytes(StandardCharsets.UTF_8);
        messageLength = _msg.length;

        out.writeInt(messageLength);
        out.write(_msg, 0, messageLength);
    }

    CInfoBlock() {
        b_type = CBlockType.Info;
    }
}
