package com.writzx.filtranet;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.common.primitives.Ints;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Filtranet.MainActivity";
    public static final int OPEN_FILE = 69;
    public static final int CREATE_FILE = 70;
    public static String filesDir = "";
    public static String sendCache = "";
    public static String receiveCache = "";
    public static FileItemAdapter adapter;

    public interface BlockListener {
        void blockReceived(BlockHolder blockHolder);
    }

    public static WeakReference<Context> context;

    private FloatingActionButton fab;

    private Button sendBtn;
    private Button recvBtn;

    private EditText ipAddress;

    String ip;

    private BlockSender sender;
    private BlockReceiver receiver;
    // private ReceiveTask receiveTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = new WeakReference<>((Context) this);
        adapter = new FileItemAdapter(this, new ArrayList<ListItem>());
        filesDir = getFilesDir().getAbsolutePath() + File.separator;
        sendCache = filesDir + "blocks" + File.separator + "send";
        receiveCache = filesDir + "blocks" + File.separator + "recv";

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

                startActivityForResult(new Intent(MainActivity.this, FileListActivity.class)
                        .putExtra(FileListActivity.FILE_CREATE_MODE_KEY, false), OPEN_FILE);
            }
        });

        recvBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        sender = new BlockSender();
        sender.start();

        receiver = new BlockReceiver(sender);
        receiver.listeners.add(listener);
        receiver.start();
    }

    BlockListener listener = new BlockListener() {
        ArrayList<Integer> req_uids = new ArrayList<>();
        ArrayList<Integer> req_file_uids = new ArrayList<>();

        @Override
        public void blockReceived(final BlockHolder blockHolder) {
            switch (blockHolder.block.b_type) {
                case Meta:
                    final CMetaBlock metaBlock = (CMetaBlock) blockHolder.block;
                    Log.i(TAG, "Filename: " + metaBlock.filename);
                    Log.i(TAG, "Filesize: " + metaBlock.length + "");
                    Log.i(TAG, "CUIDBlock uid: " + Utils.toHex(metaBlock.attached_uid));
                    Log.i(TAG, "MimeType: " + metaBlock.mimeType);

                    showSnackbar("Meta Block received!", FileListActivity.LONG_SNACK);

                    final String sb = "IP Address: " + blockHolder.ip +
                            "\nFile Name: " + metaBlock.filename +
                            "\nFile Size: " + metaBlock.length +
                            "\nCUIDBlock UID: " + Utils.toHex(metaBlock.attached_uid) +
                            "\nMime Type: " + metaBlock.mimeType;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.context.get());
                            final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case DialogInterface.BUTTON_POSITIVE:
                                            //todo show save file dialog

                                            req_uids.add(metaBlock.attached_uid);
                                            try {
                                                sender.requestUIDBlock(blockHolder.ip, metaBlock.attached_uid);
                                            } catch (IOException | InterruptedException e) {
                                                e.printStackTrace();
                                            }

                                            break;
                                        case DialogInterface.BUTTON_NEGATIVE:
                                            break;
                                    }
                                }
                            };

                            builder.setMessage(sb).setTitle("Accept file?").setPositiveButton("Accept", listener).setNegativeButton("Reject", listener).create().show();
                        }
                    });
                    break;
                case File:
                    CFileBlock fb = (CFileBlock) blockHolder.block;
                    if (fb.valid && req_file_uids.contains(fb.uid)) { // not available locally if not valid
                        showSnackbar("File Block received! UID: " + Utils.toHex(fb.uid), FileListActivity.LONG_SNACK);
                        // todo attach FileItem.CFile and save to file
                    }
                    break;
                case UID:
                    CUIDBlock ublk = (CUIDBlock) blockHolder.block;

                    showSnackbar("UID Block received : " + Utils.toHex(ublk.uid), FileListActivity.LONG_SNACK);

                    if (sender.getLocal(ublk.uid, CBlockType.UID) == null) {
                        if (req_uids.contains(ublk.uid)) {
                            try {
                                CUIDBlock.save(ublk, receiveCache);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            req_uids.remove(Integer.valueOf(ublk.uid));
                            req_file_uids.addAll(ublk.uids);
                            try {
                                sender.requestFileBlock(blockHolder.ip, false, Ints.toArray(ublk.uids));
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }

                            if (ublk.uid != ublk.next_uid) {
                                req_uids.add(ublk.next_uid);
                                try {
                                    sender.requestUIDBlock(blockHolder.ip, ublk.next_uid);
                                } catch (IOException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            // do something with unrequested block
                        }
                    } else {
                        showSnackbar("duplicate uid, not added", FileListActivity.LONG_SNACK);
                    }
                    break;
                case Info:
                    CInfoBlock infBlock = (CInfoBlock) blockHolder.block;
                    showSnackbar("Info Block received!", FileListActivity.LONG_SNACK);

                    switch (infBlock.message) {
                        case "CUIDBlock":
                            if (infBlock.info_code == CInfoBlock.INFO_ACK) {
                                for (int i = 0; i < infBlock.length / 4; i++) {
                                    try {
                                        sender.queueUIDBlock(blockHolder.ip, infBlock.uids[i]);
                                    } catch (InterruptedException | IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            break;
                        case "CFileBlock":
                            // todo handle INFO_ACK and INFO_NACK (verify)
                            for (int i = 0; i++ < infBlock.length / 4; i++) {
                                try {
                                    sender.queueFileBlock(blockHolder.ip, infBlock.uids[i]);
                                } catch (InterruptedException | IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                        default:
                            showSnackbar("BLOCK FAIL!", FileListActivity.LONG_SNACK);
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case OPEN_FILE:
                    FileItem p = data.getParcelableExtra(FileListActivity.RESULT_KEY);

                    try {
                        sender.queueFile(ip, p.getFile());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case CREATE_FILE:
                    break;
            }
        }
    }

    public static void showSnackbar(String msg, @FileListActivity.Duration int duration) {
        Log.i(TAG, "Snackbar: " + msg);
        View view = ((Activity) MainActivity.context.get()).getWindow().getDecorView().findViewById(android.R.id.content).findViewById(R.id.main_fab);

        Snackbar.make(view, msg, duration).show();
    }

    @Override
    protected void onDestroy() {
        sender.stop();
        receiver.stop();

        super.onDestroy();

        ((ActivityManager) this.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
    }
}
