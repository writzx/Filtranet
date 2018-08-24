package com.writzx.filtranet;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;
    private static final String TAG = "Filtranet";

    public static WeakReference<Context> context;

    private Button sendBtn;
    private Button recvBtn;
    private Button init;
    private EditText ipAddress;

    public MainActivity() {
    }

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
            }
        });

        sendBtn = findViewById(R.id.sendFile);
        recvBtn = findViewById(R.id.recvFile);
        ipAddress = findViewById(R.id.ipAddress);
        init = findViewById(R.id.initBtn);

        context = new WeakReference<>((Context) this);

        init.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        recvBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RecvTask().execute();
            }
        });
    }

    private class RecvTask extends AsyncTask<Void, Void, CFileBlock> {

        @Override
        protected CFileBlock doInBackground(Void... voids) {
            return null;
        }

        @Override
        protected void onPostExecute(CFileBlock s) {
            super.onPostExecute(s);
        }
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
                        new PrepFile().execute(uri);
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

    private class PrepFile extends AsyncTask<Uri, String, CFile> {
        @Override
        protected CFile doInBackground(Uri... uris) {
            // todo incorporate method here and show progress
            return openFileRead(uris[0]);
        }

        @Override
        protected void onPostExecute(CFile cFile) {
            super.onPostExecute(cFile);
            // todo send file metadata and then the file
        }
    }

    CFile openFileRead(Uri uri) {
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(uri, null, null, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

                ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
                if (pfd == null) {
                    return null;
                }
                FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());

                return new CFile(fis, displayName, resolver.getType(uri));
            } else {
                Log.e(TAG, "URI Error: Could not resolve cursor!");
                return null;
            }
        } catch (IOException ex) {
            Log.e(TAG, "URI Error: Could not resolve stream!");
            return null;
        } finally {
            cursor.close();
        }

//        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//        StringBuilder stringBuilder = new StringBuilder();
//        String line;
//        try {
//            while ((line = reader.readLine()) != null) {
//                stringBuilder.append(line);
//            }
//            inputStream.close();
//        } catch (IOException ex) {
//            Log.e(TAG, "File Error: Could not open file!");
//            return;
//        }
//        // return stringBuilder.toString();
//
//        Log.i(TAG, "File Data: " + stringBuilder.toString());
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
