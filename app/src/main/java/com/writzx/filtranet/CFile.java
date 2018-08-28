package com.writzx.filtranet;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;

public class CFile {
    CMetaBlock metaBlock;
    List<CFileBlock> blocks = new ArrayList<>();

    FileDescriptor fd;
    // should contain all other attribute data including path

    CFile(FileDescriptor fd) {
        this.fd = fd;
        metaBlock = new CMetaBlock();
    }
}
