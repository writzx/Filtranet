package com.writzx.filtranet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;

    private static final int PROGRESS_MSG = 0xffff;

    private static final String TAG = "Filtranet";

    public static WeakReference<Context> context;

    public TextView console;

    public class ProgressHandler extends Handler {
        String status;
        int bytesRead;
        int totalBytes;

        @Override
        public void handleMessage(Message msg) {
            removeMessages(PROGRESS_MSG);

            float percent = bytesRead / totalBytes;
            console.setText(String.format(Locale.ENGLISH, "Status: %s;   %d/%d;   %.2f\n", status, bytesRead, totalBytes, 100 * percent));
        }
    }

    public ProgressHandler progressHandler = new ProgressHandler();

    public void selectFileRead() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("*/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    private void selectFileWrite(String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // todo mime type
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                selectFileWrite("fuckingFile.txt");
//                selectFileRead();

                selectFileRead();
            }
        });

        console = findViewById(R.id.console);

        context = new WeakReference<>((Context) this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case READ_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = null;
                    if (resultData != null) {
                        uri = resultData.getData();
                        // Log.i(TAG, "Uri: " + uri.toString());
                        final FileProcessor proc = new FileProcessor();

                        proc.listener = new FileProcessor.Listener() {
                            @Override
                            public void init() {
                                console.append("Initializing...\n");
                            }

                            @Override
                            public void complete(CFile file) {
                                console.append("Complete\n");
                            }

                            @Override
                            public void reportProgress(String status, int bytesRead, int totalBytes) {
                                progressHandler.status = status;
                                progressHandler.bytesRead = bytesRead;
                                progressHandler.totalBytes = totalBytes;

                                progressHandler.sendEmptyMessage(PROGRESS_MSG);
                            }

                            @Override
                            public void cancel() {

                            }
                        };

                        proc.execute(uri);
                    }
                }
                break;
            case WRITE_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = null;
                    if (resultData != null) {
                        uri = resultData.getData();
                        // Log.i(TAG, "Uri: " + uri.toString());
                        // writeFile(uri);
                    }
                }
                break;
        }
    }

    FileOutputStream openFileWrite(Uri uri) {
//        OutputStream outputStream = null;
//        try {
//            outputStream = getContentResolver().openOutputStream(uri, "w");
//        } catch (IOException ex) {
//            Log.e(TAG, "URI Error: Could not resolve stream!");
//            return;
//        }
//
//        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
//
//        try {
//            writer.write("THIS IS A FUCKING NEW FILE CREATED BY THE APP FILTRANET!");
//            writer.flush();
//            writer.close();
//            // outputStream.close();
//        } catch (IOException ex) {
//            Log.e(TAG, "File Error: Could not open file!");
//            return;
//        }

        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
            return pfd != null ? new FileOutputStream(pfd.getFileDescriptor()) : null;
        } catch (IOException ex) {
            Log.e(TAG, "URI Error: Could not resolve stream!");
            return null;
        }
//        Log.i(TAG, "File Write successful!");
    }
}
