package com.writzx.filtranet;


import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.res.ResourcesCompat;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileListItem implements ListItem {
    private Uri uri;
    private Uri iconUri;
    private Drawable icon;
    private CFile file;

    private String filename;
    private String path;
    private int filesize;
    private Date dateadded;
    private String mimetype;

    public static final SimpleDateFormat dateformat = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());

    public FileListItem(Uri uri, int filesize, Uri iconUri) {
        File file = FileUtils.getFile(MainActivity.context.get(), uri);

        this.file = null;
        this.uri = uri;
        this.filename = file.getName();
        this.path = file.getAbsolutePath();
        this.filesize = filesize;
        this.dateadded = new Date(System.currentTimeMillis());
        this.mimetype = FileUtils.getMimeType(file);
        this.iconUri = iconUri;

        prepMetaBlock();

        generateThumb();
    }

    public FileListItem(CFile cfile, Uri uri, int filesize, Uri iconUri) {
        File file = FileUtils.getFile(MainActivity.context.get(), uri);

        this.file = cfile;
        this.uri = uri;
        this.filename = file.getName();
        this.path = file.getPath();
        this.filesize = filesize;
        this.dateadded = new Date(System.currentTimeMillis());
        this.mimetype = FileUtils.getMimeType(file);
        this.iconUri = iconUri;

        prepMetaBlock();

        generateThumb();
    }

    public FileListItem(CFile cfile, Uri uri, int filesize) {
        File file = FileUtils.getFile(MainActivity.context.get(), uri);

        this.file = cfile;
        this.uri = uri;
        this.filename = file.getName();
        this.path = file.getPath();
        this.filesize = filesize;
        this.dateadded = new Date(System.currentTimeMillis());
        this.mimetype = FileUtils.getMimeType(file);

        prepMetaBlock();

        generateThumb();
    }

    public String getFileName() {
        return filename;
    }

    public int getFileSize() {
        return filesize;
    }

    public Date getDateAdded() {
        return dateadded;
    }

    public Uri getUri() {
        return uri;
    }

    public String getMimeType() {
        return mimetype;
    }

    public CFile getFile() {
        return file;
    }

    public String getPath() {
        return path;
    }

    // not to be run in ui thread
    public void generateThumb() {

        if (iconUri != null) {
            icon = BitmapDrawable.createFromPath(FileUtils.getPath(MainActivity.context.get(), iconUri));
        }

        // get thumbnail
        if (icon == null) {
            icon = FileUtils.getThumbnail(MainActivity.context.get(), uri);
            // todo save and set iconUri
        }

        // get icon
        if (icon == null) {
            icon = FileUtils.getIcon(MainActivity.context.get(), uri, mimetype);
        }

        // get default unknown file icon
        if (icon == null) {
            icon = ResourcesCompat.getDrawable(MainActivity.context.get().getResources(), R.drawable.ic_unknown_file, null);
        }
    }

    public Drawable getIcon() {
        return icon;
    }

    public void prepMetaBlock() {
        file.metaBlock.length = getFileSize();
        file.metaBlock.filename = getFileName();
        file.metaBlock.mimeType = getMimeType();
    }

    @Override
    public int getType() {
        return ListItem.TYPE_FILE;
    }
}
