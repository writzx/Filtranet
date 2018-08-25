package com.writzx.filtranet;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;

class CFile {
    CMetaBlock metaBlock;

    List<CFileBlock> blocks = new ArrayList<>();
    FileDescriptor fd;
    // should contain all other attribute data including path

//    CFile(FileInputStream file, String filename, String mimeType) throws IOException {
//
//    }

    CFile(FileDescriptor fd) {
        this.fd = fd;
    }
}
