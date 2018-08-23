package com.writzx.filtranet;

import android.app.Activity;
import android.content.ContentResolver;
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

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32;

public class MainActivity extends AppCompatActivity {
    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;
    private static final String TAG = "Filtranet";

    private static final int BLOCK_SIZE = 1024;

    private Button sendBtn;
    private Button recvBtn;

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


        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFileRead();
            }
        });

        recvBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
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

    private class PrepFile extends AsyncTask<Uri, String, SFile> {
        @Override
        protected SFile doInBackground(Uri... uris) {
            // todo incorporate method here and show progress
            return openFileRead(uris[0]);
        }
    }

    private class SendAsync extends AsyncTask<SFileBlock, String, String> {
        @Override
        protected String doInBackground(SFileBlock... sFileBlocks) {
            try {
                FileInputStream fis;
                for (SFileBlock block : sFileBlocks) {
                    fis = new FileInputStream(block.sfile.fd);

                    fis.skip(block.offset);

                    byte[] data = new byte[block.length];

                    fis.read(data, 0, block.length);
                    // todo send
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    private class SFileBlock {
        String uid;
        long offset;
        int length;
        long crc;

        SFile sfile;

        // byte[] data; // not stored to reduce ram usage
    }

    private class SFile {
        long length;
        String filename;
        String mimeType;

        List<SFileBlock> blocks;
        FileDescriptor fd;
        // should contain all other attribute data including path

        private SFile(FileInputStream file, String filename, String mimeType) throws IOException {
            fd = file.getFD();

            int bytesRead;

            byte[] buffer = new byte[BLOCK_SIZE];

            CRC32 crc32 = new CRC32();

            for (length = 0; (bytesRead = file.read(buffer, 0, BLOCK_SIZE)) != -1; length += bytesRead) {
                SFileBlock sblock = new SFileBlock();

                sblock.uid = UUID.randomUUID().toString();

                sblock.offset = length;
                sblock.length = bytesRead;

                crc32.update(buffer);
                sblock.crc = crc32.getValue();
                crc32.reset();

                sblock.sfile = this;

                blocks.add(sblock);
            }

            this.filename = filename;
            this.mimeType = mimeType;
        }
    }

    SFile openFileRead(Uri uri) {
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

                return new SFile(fis, displayName, resolver.getType(uri));
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
