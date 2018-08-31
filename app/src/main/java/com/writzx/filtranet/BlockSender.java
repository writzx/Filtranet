package com.writzx.filtranet;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockSender implements Runnable {
    private Thread thread;
    private final LinkedBlockingQueue<BlockHolder> queue;

    public BlockSender(LinkedBlockingQueue<BlockHolder> queue) throws SocketException {
        this.queue = queue;
        thread = new Thread(this);
    }

    @Override
    public void run() {
        try {
            while (true) {
                send(queue.take());
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private void send(BlockHolder b) throws IOException {
        UDPSender.getInstance().send(b.dest_ip, b.block);
    }

    public Thread getThread() {
        return thread;
    }
}
