package com.writzx.filtranet;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;

public class ReceiveTask extends AsyncTask<Void, BlockHolder, CFile> {
    private static final String TAG = "ReceiveTask";
    public ArrayList<MainActivity.BlockListener> listeners = new ArrayList<>();
    public BlockSender sender;

    public ReceiveTask(BlockSender sender) {
        this.sender = sender;
    }

    @Override
    protected CFile doInBackground(Void... voids) {
        try {
            UDPReceiver receiver = UDPReceiver.getInstance();
            while (true) {
                publishProgress(receiver.receive());
                try {
                    BlockHolder b;
                    while ((b = sender.localBlocks.take()) != null) {
                        publishProgress(b);
                    }
                } catch (InterruptedException ignored) {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(final BlockHolder... values) {
        super.onProgressUpdate(values);

        for (MainActivity.BlockListener bl : listeners) {
            bl.blockReceived(values[0]);
        }
    }
}
