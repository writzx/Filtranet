package com.writzx.filtranet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Filtranet";

    public static WeakReference<Context> context;

    private static LinkedBlockingQueue<BlockHolder> queue = new LinkedBlockingQueue<>();

    private FloatingActionButton fab;

    private Button sendBtn;
    private Button recvBtn;

    private EditText ipAddress;

    String ip;

    public static FileListItem item;

    BlockSender sender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = new WeakReference<>((Context) this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = findViewById(R.id.main_fab);
        sendBtn = findViewById(R.id.sendFile);
        recvBtn = findViewById(R.id.recvFile);
        ipAddress = findViewById(R.id.ipAddress);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ip = ipAddress.getText().toString();

                Intent i = new Intent(MainActivity.this, FileListActivity.class);
                startActivityForResult(i, 69);
            }
        });

        recvBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReceiveTask receiveTask = new ReceiveTask();
                receiveTask.execute();
            }
        });

        try {
            sender = new BlockSender(queue);
            sender.getThread().start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private static class ReceiveTask extends AsyncTask<Void, CBlock, CFile> {
        boolean receiving = false;

        @Override
        protected CFile doInBackground(Void... voids) {
            try {
                UDPReceiver receiver = UDPReceiver.getInstance();
                publishProgress(receiver.receive());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(CBlock... values) {
            super.onProgressUpdate(values);

            if (values[0].b_type == CBlockType.Meta && !receiving) {
                receiving = true;

                showSnackbar("Meta Block received!", FileListActivity.LONG_SNACK);

                // todo print details of block
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 69:
                    try {
                        queue.put(new BlockHolder(ip, item.getFile().metaBlock));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case 70:
                    break;
            }
        }
    }

    public static void showSnackbar(String msg, @FileListActivity.Duration int duration) {
        View listView = ((Activity) MainActivity.context.get()).getWindow().getDecorView().findViewById(android.R.id.content).findViewById(R.id.main_fab);

        Snackbar.make(listView, msg, duration).show();
    }
}
