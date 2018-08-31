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

public class FileProcessor extends AsyncTask<Void, Integer, FileListItem> {
    private static final String TAG = "FileProcessor";
    private Uri uri;
    private int index;
    private long startTime;

    private FileProcessor(Uri uri) {
        this.uri = uri;
    }

    public static FileProcessor create(Uri uri) {
        return new FileProcessor(uri);
    }

    @Override
    protected FileListItem doInBackground(Void... voids) {
        if (isCancelled()) return null;

        ContentResolver resolver = FileListActivity.context.get().getContentResolver();
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

            for (len = 0; (bytesRead = fis.read(buffer, 0, CBlock.BLOCK_LENGTH)) != -1 && !isCancelled(); len += bytesRead) {
                CFileBlock cblock = new CFileBlock();

                cblock.uid = UID.generate();

                cblock.offset = len;
                cblock.length = bytesRead;

                crc.update(buffer, 0, buffer.length);
                cblock.crc = crc.getShortValue();
                crc.reset();

                cblock.cfile = cfile;

                cfile.addBlock(cblock);

                publishProgress(len, totalLen);
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

        // check if file permissions are present
        try {
            File f = FileUtils.getFile(FileListActivity.context.get(), uri);

            // cancel the task as it already exists
            if (FileListActivity.adapter.contains(f.getAbsolutePath())) {
                FileListActivity.showSnackbar("Unable to add file!\nAlready present in the list!", FileListActivity.LONG_SNACK);

                cancel(true);
                return;
            }

            ProgressListItem p = new ProgressListItem(f.getName(), f.getAbsolutePath());
            p.setStatus("Initializing...");

            index = FileListActivity.adapter.getCount();
            FileListActivity.adapter.insert(p, index);
        } catch (SecurityException se) {
            cancel(true);
            FileListActivity.showSnackbar("Could not load file from chooser, use a different file provider.", FileListActivity.LONG_SNACK);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        if (values.length == 2) {
            int byteRead = values[0];
            int total = values[1];

            ProgressListItem p = (ProgressListItem) FileListActivity.adapter.getItem(index);

            if (p != null) {
                p.setStatus("Processing...");
                p.setBytesRead(byteRead);
                p.setTotalLength(total);
            }

            FileListActivity.adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPostExecute(FileListItem fileListItem) {
        super.onPostExecute(fileListItem);
        long elapsed = System.currentTimeMillis() - startTime;

        int totalBytes = fileListItem.getFileSize();

        ListItem litem = FileListActivity.adapter.getItem(index);
        if (litem != null) {
            ProgressListItem pcitem = (ProgressListItem) litem;
            pcitem.setBytesRead(totalBytes);
            pcitem.setTotalLength(totalBytes);

            pcitem.setStatus("Complete.");

            FileListActivity.adapter.remove(pcitem);
            FileListActivity.adapter.insert(fileListItem, index);

            Log.e(TAG, "ELAPSED TIME: " + elapsed);
        }
    }
}
