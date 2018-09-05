package com.writzx.filtranet;

import java.io.IOException;
import java.util.ArrayList;

public class BlockReceiver implements Runnable {
    private static final String TAG = "BlockReceiver";
    public ArrayList<MainActivity.BlockListener> listeners;
    private final BlockSender sender;
    private boolean started = false;
    private Thread thread;

    public BlockReceiver(BlockSender sender) {
        this.sender = sender;
        listeners = new ArrayList<>();
        thread = new Thread(this);
    }

    @Override
    public void run() {
        while (started) {
            try {
                onReceive(UDPReceiver.getInstance().receive());
                BlockHolder b;
                while ((b = sender.localBlocks.poll()) != null) {
                    onReceive(b);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void onReceive(final BlockHolder blk) {
        for (final MainActivity.BlockListener bl : listeners) {
            bl.blockReceived(blk);
        }
    }

    public void start() {
        started = true;
        thread.start();
    }

    public void stop() {
        started = false;
    }
}
