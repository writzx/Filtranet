package com.writzx.filtranet;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileProcessor extends AsyncTask<Void, String, FileListItem> {
    private static final String TAG = "FileProcessor";
    public Uri uri;
    public int index;
    public long startTime;

    public FileProcessor(Uri uri) {
        this.uri = uri;
    }

    @Override
    protected FileListItem doInBackground(Void... voids) {
        if (isCancelled()) return null;

        ContentResolver resolver = MainActivity.context.get().getContentResolver();
        CRC16 crc = new CRC16();

        startTime = System.currentTimeMillis();

        try {
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
            if (pfd == null) {
                return null;
            }
            FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
            int totalLen = fis.available();

            CFile cfile = new CFile(fis.getFD());

            int bytesRead, len;

            byte[] buffer = new byte[CBlock.BLOCK_LENGTH];

            // todo generate UID blocks inside the loop (also enqueue the blocks)

            for (len = 0; (bytesRead = fis.read(buffer, 0, CBlock.BLOCK_LENGTH)) != -1 && !isCancelled(); len += bytesRead) {
                CFileBlock cblock = new CFileBlock();

                cblock.uid = UID.generate();

                cblock.offset = len;
                cblock.length = bytesRead;

                crc.update(buffer, 0, buffer.length);
                cblock.crc = crc.getShortValue();
                crc.reset();

                cblock.cfile = cfile;

                cfile.blocks.add(cblock);

                publishProgress("" + len, "" + totalLen);
            }

            return isCancelled() ? null : new FileListItem(cfile, uri, totalLen);
        } catch (IOException ex) {
            Log.e(TAG, "URI Error: Could not resolve stream!");
            return null;
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        File f = FileUtils.getFile(MainActivity.context.get(), uri);

        // cancel the task as it already exists
        if (MainActivity.adapter.contains(f.getAbsolutePath())) {
            MainActivity.showSnackbar("Unable to add file!\nAlready present in the list!", MainActivity.LONG_SNACK);

            cancel(true);
            return;
        }

        ProgressListItem p = new ProgressListItem(f.getName(), f.getAbsolutePath());
        p.setStatus("Initializing...");

        index = MainActivity.adapter.getCount();
        MainActivity.adapter.insert(p, index);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);

        if (values.length == 2) {
            int byteRead = Integer.valueOf(values[0]);
            int total = Integer.valueOf(values[1]);

            ProgressListItem p = (ProgressListItem) MainActivity.adapter.getItem(index);

            if (p != null) {
                p.setStatus("Processing...");
                p.setBytesRead(byteRead);
                p.setTotalLength(total);
            }

            MainActivity.adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPostExecute(FileListItem fileListItem) {
        super.onPostExecute(fileListItem);
        long elapsed = System.currentTimeMillis() - startTime;

        int totalBytes = fileListItem.getFileSize();

        ListItem litem = MainActivity.adapter.getItem(index);
        if (litem != null) {
            ProgressListItem pcitem = (ProgressListItem) litem;
            pcitem.setBytesRead(totalBytes);
            pcitem.setTotalLength(totalBytes);

            pcitem.setStatus("Complete.");

            MainActivity.adapter.remove(pcitem);
            MainActivity.adapter.insert(fileListItem, index);

            Log.e(TAG, "ELAPSED TIME: " + elapsed);
        }
    }
}
