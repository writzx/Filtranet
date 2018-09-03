package com.writzx.filtranet;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class UDPReceiver implements Closeable {
    private static UDPReceiver instance;
    DatagramSocket socket;

    public static final int MAX_TRANSMISSION_UNIT = 1500; // MTU size for most networks
    public static final int SEND_PORT = 16969;
    public static final int RECV_PORT = 26969;

    private UDPReceiver() throws SocketException {
        this.socket = new DatagramSocket(RECV_PORT);
    }

    public static UDPReceiver getInstance() throws SocketException {
        if (instance == null) instance = new UDPReceiver();
        return instance;
    }

    public String receiveText() throws IOException {
        byte[] packetBytes = new byte[MAX_TRANSMISSION_UNIT];
        DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length);

        // instance.r_socket.setSoTimeout(1000);
        socket.receive(packet);

        String str = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        System.out.println(str);
        return str;
    }

    public BlockHolder receive() throws IOException {
        byte[] packetBytes = new byte[MAX_TRANSMISSION_UNIT];
        DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length);

        // instance.r_socket.setSoTimeout(1000);
        socket.receive(packet);

        try (ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData()); DataInputStream in = new DataInputStream(bis)) {
            CBlock block = CBlock.factory(in);
            block.read(in);

            return BlockHolder.of(packet.getAddress().getHostAddress(), block);
        }
    }

    @Override
    public void close() {
        if (socket != null) socket.close();
    }
}
