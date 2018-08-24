package com.writzx.filtranet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class UDP {
    private static UDP instance;

    private int s_port;
    private int r_port;
    private DatagramSocket s_socket;
    private DatagramSocket r_socket;

    public static final int MAX_TRANSMISSION_UNIT = 1500; // MTU size for most networks

    public static UDP it() {
        return instance;
    }

    private UDP() {
    } // disable default constructor

    public static void init(int s_port, int r_port) throws SocketException {
        if (s_port == r_port) throw new SocketException();

        if (instance != null && instance.s_socket != null) instance.s_socket.close();
        if (instance != null && instance.r_socket != null) instance.r_socket.close();

        instance = new UDP();

        instance.s_port = s_port;
        instance.r_port = r_port;

        instance.s_socket = new DatagramSocket(s_port);
        instance.r_socket = new DatagramSocket(r_port);
    }

    public static void send(String ip, CBlock block) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write(new byte[]{block.b_type.value});
        block.write(new DataOutputStream(out));

        DatagramPacket packet = new DatagramPacket(out.toByteArray(), out.size(), InetAddress.getByName(ip), instance.s_port);

        instance.s_socket.send(packet);
    }

    public static CBlock receive() throws IOException {
        byte[] packetBytes = new byte[MAX_TRANSMISSION_UNIT];
        DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length);

        instance.r_socket.setSoTimeout(1000);
        instance.r_socket.receive(packet);

        try (ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData()); DataInputStream in = new DataInputStream(bis)) {
            CBlock block = CBlock.factory(in);
            block.read(in);

            return block;
        }
    }

    public static void sendText(String ip, String str) throws IOException {
        byte[] buf = str.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), instance.s_port);

        instance.s_socket.send(packet);
    }

    public static String receiveText() throws IOException {
        byte[] packetBytes = new byte[MAX_TRANSMISSION_UNIT];
        DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length);

        instance.r_socket.setSoTimeout(1000);
        instance.r_socket.receive(packet);

        String str = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        System.out.println(str);
        return str;
    }

    public static void deint() {
        if (instance != null && instance.s_socket != null) instance.s_socket.close();
        if (instance != null && instance.r_socket != null) instance.r_socket.close();

        instance = null;
    }
}
