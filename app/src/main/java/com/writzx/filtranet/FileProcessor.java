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

public class FileProcessor extends AsyncTask<Void, Integer, FileItem> {
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
    protected FileItem doInBackground(Void... voids) {
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

            CFile cfile = new CFile(uri);

            int bytesRead, len;

            byte[] buffer = new byte[CBlock.MAX_BLOCK_LENGTH];

            for (len = 0; (bytesRead = fis.read(buffer, 0, CBlock.MAX_BLOCK_LENGTH)) != -1 && !isCancelled(); len += bytesRead) {
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

            // save the uid blocks in send cache
            CUIDBlock ublk = cfile.metaBlock.uid_block;

            do {
                CUIDBlock.save(ublk, MainActivity.sendCache);
            } while ((ublk = ublk.next) != null);

            return isCancelled() ? null : new FileItem(cfile, uri, totalLen);
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
            if (MainActivity.adapter.contains(f.getAbsolutePath())) {
                FileListActivity.showSnackbar("Unable to add file!\nAlready present in the list!", FileListActivity.LONG_SNACK);

                cancel(true);
                return;
            }

            ProgressItem p = new ProgressItem(f.getName(), f.getAbsolutePath());
            p.setStatus("Initializing...");

            index = MainActivity.adapter.getCount();
            MainActivity.adapter.insert(p, index);
        } catch (SecurityException se) {
            cancel(true);
            FileListActivity.showSnackbar("Could not load file from chooser, use a different file provider.", FileListActivity.LONG_SNACK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        if (values.length == 2) {
            int byteRead = values[0];
            int total = values[1];

            ProgressItem p = (ProgressItem) MainActivity.adapter.getItem(index);

            if (p != null) {
                p.setStatus("Processing...");
                p.setBytesRead(byteRead);
                p.setTotalLength(total);
            }

            MainActivity.adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPostExecute(FileItem fileItem) {
        super.onPostExecute(fileItem);
        long elapsed = System.currentTimeMillis() - startTime;

        int totalBytes = fileItem.getFileSize();

        ListItem litem = MainActivity.adapter.getItem(index);
        if (litem != null) {
            ProgressItem pcitem = (ProgressItem) litem;
            pcitem.setBytesRead(totalBytes);
            pcitem.setTotalLength(totalBytes);

            pcitem.setStatus("Complete.");

            MainActivity.adapter.remove(pcitem);
            MainActivity.adapter.insert(fileItem, index);

            Log.e(TAG, "ELAPSED TIME: " + elapsed);
        }
    }
}
