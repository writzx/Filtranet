package com.writzx.filtranet;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class UDPSender implements Closeable {
    private static UDPSender instance;
    DatagramSocket socket;

    public static final int MAX_TRANSMISSION_UNIT = 1500; // MTU size for most networks
    public static final int SEND_PORT = 16969;
    public static final int RECV_PORT = 26969;

    private UDPSender() throws SocketException {
        this.socket = new DatagramSocket(SEND_PORT);
    }

    public static UDPSender getInstance() throws SocketException {
        if (instance == null) instance = new UDPSender();
        return instance;
    }

    public void send(BlockHolder bh) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write(new byte[]{bh.block.b_type.value});
        bh.block.write(new DataOutputStream(out));

        DatagramPacket packet = new DatagramPacket(out.toByteArray(), out.size(), InetAddress.getByName(bh.ip), RECV_PORT);

        socket.send(packet);
    }

    public void sendText(String ip, String str) throws IOException {
        byte[] buf = str.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), RECV_PORT);

        socket.send(packet);
    }

    @Override
    public void close() {
        if (socket != null) socket.close();
    }
}
