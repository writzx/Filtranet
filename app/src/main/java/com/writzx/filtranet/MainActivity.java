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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;
    private static final String TAG = "Filtranet";

    private static final int BLOCK_LENGTH = 1024;

    private static final Random random = new Random();

    static short generateRandomUID() {
        return (short) random.nextInt(Short.MAX_VALUE + 1);
    }

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

        @Override
        protected void onPostExecute(SFile sFile) {
            super.onPostExecute(sFile);
            // todo send file metadata and then the file
        }
    }

    private interface SBlock {
        void read(DataInputStream in) throws IOException;

        void write(DataOutputStream out) throws IOException;
    }

    private class SFileBlock implements SBlock {
        short uid;
        long offset;
        int crc;
        int length;

        SFile sfile;

        @Override
        public void write(DataOutputStream out) throws IOException {
            // todo check sfile and throw filenotfoundexception appropriately
            byte[] data = new byte[length];

            try (FileInputStream fis = new FileInputStream(sfile.fd)) {
                fis.skip(offset);
                fis.read(data, 0, length);
            }

            out.writeShort(uid);

            out.writeLong(offset);
            out.writeLong(crc);
            out.writeLong(length);

            out.write(data, 0, length);

        }

        @Override
        public void read(DataInputStream in) throws IOException {
            // todo check sfile and throw filenotfoundexception appropriately
            uid = in.readShort();

            offset = in.readLong();
            crc = in.readInt();
            length = in.readInt();

            byte[] data = new byte[length];
            in.read(data, 0, length);

            // todo crc and accept/discard

            try (FileOutputStream fos = new FileOutputStream(sfile.fd); FileChannel ch = fos.getChannel()) {
                ch.position(offset);
                ch.write(ByteBuffer.wrap(data));
            }
            // write to SFile
        }
    }

    private class SMetaBlock implements SBlock {
        long length;
        short attached_uid; // uid of the attached uid block

        short nameLength; // not used outside
        short mimeTypeLength; // not used outside

        String filename;
        String mimeType;

        @Override
        public void read(DataInputStream in) throws IOException {
            length = in.readLong();
            attached_uid = in.readShort();

            nameLength = in.readShort();
            mimeTypeLength = in.readShort();

            byte[] _name = new byte[nameLength];
            byte[] _mime = new byte[mimeTypeLength];

            in.read(_name, 0, nameLength);
            in.read(_mime, 0, mimeTypeLength);

            filename = new String(_name, StandardCharsets.UTF_8);
            mimeType = new String(_mime, StandardCharsets.UTF_8);
        }

        @Override
        public void write(DataOutputStream out) throws IOException {
            byte[] _name = filename.getBytes(StandardCharsets.UTF_8);
            byte[] _mime = filename.getBytes(StandardCharsets.UTF_8);

            nameLength = (short) _name.length;
            mimeTypeLength = (short) _mime.length;

            out.writeLong(length);
            out.writeShort(attached_uid);

            out.writeShort(nameLength);
            out.writeShort(mimeTypeLength);

            out.write(_name, 0, nameLength);
            out.write(_mime, 0, mimeTypeLength);
        }
    }

    private class SInfoBlock implements SBlock {
        short attached_uid; // uid of the attached block
        int info_code;
        int messageLength; // not to be used outside of this class
        String message;

        @Override
        public void read(DataInputStream in) throws IOException {
            attached_uid = in.readShort();

            info_code = in.readInt();
            messageLength = in.readInt();

            byte[] _msg = new byte[messageLength];
            in.read(_msg, 0, messageLength);

            message = new String(_msg, StandardCharsets.UTF_8);
        }

        @Override
        public void write(DataOutputStream out) throws IOException {
            out.writeShort(attached_uid);

            out.writeInt(info_code);

            byte[] _msg = message.getBytes(StandardCharsets.UTF_8);
            messageLength = _msg.length;

            out.writeInt(messageLength);
            out.write(_msg, 0, messageLength);

        }
    }

    private class SUIDBlock implements SBlock {
        short uid;
        int length; // number of bytes occupied by the next field, i.e., UIDs array.
        short[] uids;
        short next_uid = 0; // uid of the next uid block; same as uid if none (0)

        @Override
        public void read(DataInputStream in) throws IOException {
            uid = in.readShort();
            length = in.readInt();

            byte[] _uids = new byte[length];
            in.read(_uids, 0, length);

            uids = new short[length / 2];
            ByteBuffer.wrap(_uids).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(uids);

            next_uid = in.readShort();
        }

        @Override
        public void write(DataOutputStream out) throws IOException {
            out.writeShort(uid);
            length = uids.length * 2;
            out.writeInt(length);

            byte[] _uids = new byte[length];
            ByteBuffer.wrap(_uids).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(uids);

            out.write(_uids, 0, length);

            if (next_uid == 0) next_uid = uid;
            out.write(next_uid);
        }
    }

    private class SFile {
        SMetaBlock metaBlock;

        List<SFileBlock> blocks;
        FileDescriptor fd;
        // should contain all other attribute data including path

        private SFile(FileInputStream file, String filename, String mimeType) throws IOException {
            fd = file.getFD();

            int bytesRead;

            CRC16 crc = new CRC16();

            metaBlock = new SMetaBlock();
            byte[] buffer = new byte[BLOCK_LENGTH];

            for (metaBlock.length = 0; (bytesRead = file.read(buffer, 0, BLOCK_LENGTH)) != -1; metaBlock.length += bytesRead) {
                SFileBlock sblock = new SFileBlock();

                sblock.uid = generateRandomUID();

                sblock.offset = metaBlock.length;
                sblock.length = bytesRead;

                crc.update(buffer, 0, buffer.length);
                sblock.crc = (short) crc.getValue();
                crc.reset();

                sblock.sfile = this;

                blocks.add(sblock);
            }

            this.metaBlock.filename = filename;
            this.metaBlock.mimeType = mimeType;
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
