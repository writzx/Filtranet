package com.writzx.filtranet;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;

public class CFile implements Parcelable {
    CMetaBlock metaBlock;
    List<CFileBlock> blocks = new ArrayList<>();

    FileDescriptor fd;
    // should contain all other attribute data including path

    CFile(FileDescriptor fd) {
        this.fd = fd;
        metaBlock = new CMetaBlock();
    }

    private CFile() {
    }

    protected CFile(Parcel in) {
        metaBlock = in.readParcelable(CMetaBlock.class.getClassLoader());
        in.readTypedList(blocks, CFileBlock.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(metaBlock, flags);
        dest.writeTypedList(blocks);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CFile> CREATOR = new Creator<CFile>() {
        @Override
        public CFile createFromParcel(Parcel in) {
            return new CFile(in);
        }

        @Override
        public CFile[] newArray(int size) {
            return new CFile[size];
        }
    };

    public static CFile createNew() {
        return new CFile();
    }

    void addBlock(CFileBlock block) {
        blocks.add(block);
        metaBlock.uid_block.addUID(block.uid);
    }

    void removeBlock(int index) {
        CFileBlock blk = blocks.remove(index);
        metaBlock.uid_block.removeUID(blk.uid);
    }
}
