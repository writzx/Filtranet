package com.writzx.filtranet;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;

public class FileProcessor extends AsyncTask<Uri, String, CFile> {
    private static final String TAG = "FileProcessor";

    public interface Listener {
        void init();

        void complete(CFile file);

        void reportProgress(String status, int bytesRead, int totalBytes);

        void cancel();
    }

    public Listener listener = null;


    @Override
    protected CFile doInBackground(Uri... uris) {
        Uri uri = uris[0]; // process only the first uri

        ContentResolver resolver = MainActivity.context.get().getContentResolver();
        CRC16 crc = new CRC16();

        try {
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
            if (pfd == null) {
                return null;
            }
            FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
            int totalLen = fis.available();

            CFile cfile = new CFile(fis.getFD());
            CMetaBlock metaBlock = new CMetaBlock();

            int bytesRead;

            byte[] buffer = new byte[CBlock.BLOCK_LENGTH];

            for (metaBlock.length = 0; (bytesRead = fis.read(buffer, 0, CBlock.BLOCK_LENGTH)) != -1; metaBlock.length += bytesRead) {
                CFileBlock cblock = new CFileBlock();

                cblock.uid = UID.generate();

                cblock.offset = metaBlock.length;
                cblock.length = bytesRead;

                crc.update(buffer, 0, buffer.length);
                cblock.crc = crc.getShortValue();
                crc.reset();

                cblock.cfile = cfile;

                cfile.blocks.add(cblock);

                publishProgress("Reading file into chunks...", "" + metaBlock.length, "" + totalLen);
            }

            metaBlock.filename = getFileName(resolver, uri);
            metaBlock.mimeType = resolver.getType(uri);

            return cfile;
        } catch (IOException ex) {
            Log.e(TAG, "URI Error: Could not resolve stream!");
            return null;
        }
    }

    public String getFileName(ContentResolver resolver, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (listener != null) {
            listener.init();
        }
    }

    @Override
    protected void onPostExecute(CFile cFile) {
        super.onPostExecute(cFile);

        if (listener != null) {
            listener.complete(cFile);
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);

        if (values.length == 3 && listener != null) {
            String status = values[0];
            int byteRead = Integer.valueOf(values[1]);
            int total = Integer.valueOf(values[2]);

            listener.reportProgress(status, byteRead, total);
        }
    }

    @Override
    protected void onCancelled(CFile cFile) {
        super.onCancelled(cFile);

        if (listener != null) {
            listener.cancel();
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        if (listener != null) {
            listener.cancel();
        }
    }
}
