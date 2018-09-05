package com.writzx.filtranet;


import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.res.ResourcesCompat;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileItem implements ListItem, Parcelable {
    private Uri uri;
    private Uri iconUri;

    private String filename;
    private String path;
    private int filesize;
    private Date dateadded;
    private String mimetype;

    private Drawable icon;
    private CFile file;

    public static final SimpleDateFormat dateformat = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());

    /*public FileItem(Uri uri, int filesize, Uri iconUri) {
        File file = FileUtils.getFile(FileListActivity.context.get(), uri);

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
    }*/

    public FileItem(CFile cfile, Uri uri, int filesize, Uri iconUri) {
        File file = FileUtils.getFile(FileListActivity.context.get(), uri);

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

        fixRefs();
    }

    public FileItem(CFile cfile, Uri uri, int filesize) {
        File file = FileUtils.getFile(FileListActivity.context.get(), uri);

        this.file = cfile;
        this.uri = uri;
        this.filename = file.getName();
        this.path = file.getPath();
        this.filesize = filesize;
        this.dateadded = new Date(System.currentTimeMillis());
        this.mimetype = FileUtils.getMimeType(file);

        prepMetaBlock();

        generateThumb();

        fixRefs();
    }

    protected FileItem(Parcel in) {
        uri = in.readParcelable(Uri.class.getClassLoader());
        iconUri = in.readParcelable(Uri.class.getClassLoader());
        filename = in.readString();
        path = in.readString();
        filesize = in.readInt();
        mimetype = in.readString();
        try {
            dateadded = dateformat.parse(in.readString());
        } catch (ParseException e) {
            dateadded = new Date(System.currentTimeMillis());
        }
        file = in.readParcelable(CFile.class.getClassLoader());

        fixRefs();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(uri, flags);
        dest.writeParcelable(iconUri, flags);
        dest.writeString(filename);
        dest.writeString(path);
        dest.writeInt(filesize);
        dest.writeString(mimetype);
        dest.writeString(dateformat.format(dateadded));
        dest.writeParcelable(file, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FileItem> CREATOR = new Creator<FileItem>() {
        @Override
        public FileItem createFromParcel(Parcel in) {
            return new FileItem(in);
        }

        @Override
        public FileItem[] newArray(int size) {
            return new FileItem[size];
        }
    };

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
            icon = BitmapDrawable.createFromPath(FileUtils.getPath(FileListActivity.context.get(), iconUri));
        }

        // get thumbnail
        if (icon == null) {
            icon = FileUtils.getThumbnail(FileListActivity.context.get(), uri);
            // todo save and set iconUri
        }

        // get icon
        if (icon == null) {
            icon = FileUtils.getIcon(FileListActivity.context.get(), uri, mimetype);
        }

        // get default unknown file icon
        if (icon == null) {
            icon = ResourcesCompat.getDrawable(FileListActivity.context.get().getResources(), R.drawable.ic_unknown_file, null);
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

    public void fixRefs() {
        if (file != null) {
            file.uri = uri;
            for (CFileBlock fb : file.blocks) {
                fb.cfile = file;
            }
        }
    }
}
