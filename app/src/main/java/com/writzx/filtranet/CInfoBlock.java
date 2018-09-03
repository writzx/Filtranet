package com.writzx.filtranet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

class CInfoBlock extends CBlock {
    public final static int INFO_ACK = 69;
    public final static int INFO_NACK = 70;

    int length; // number of bytes occupied by the next field, i.e., UIDs array.
    short[] uids; // uid of the attached block
    int info_code;
    int messageLength; // not to be used outside of this class
    String message;

    @Override
    public void read(DataInputStream in) throws IOException {
        length = in.readInt();

        byte[] _uids = new byte[length];
        in.read(_uids, 0, length);

        uids = new short[length / 2];
        ByteBuffer.wrap(_uids).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(uids);

        info_code = in.readInt();
        messageLength = in.readInt();

        byte[] _msg = new byte[messageLength];
        in.read(_msg, 0, messageLength);

        message = new String(_msg, StandardCharsets.UTF_8);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        length = uids.length * 2;
        out.writeInt(length);

        byte[] _uids = new byte[length];
        ByteBuffer.wrap(_uids).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(uids);

        out.write(_uids, 0, length);

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
