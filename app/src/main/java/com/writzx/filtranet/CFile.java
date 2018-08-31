package com.writzx.filtranet;

import java.io.FileDescriptor;
import java.util.LinkedList;

public class CFile {
    CMetaBlock metaBlock;
    LinkedList<CFileBlock> blocks = new LinkedList<>();

    FileDescriptor fd;
    // should contain all other attribute data including path

    CFile(FileDescriptor fd) {
        this.fd = fd;
        metaBlock = new CMetaBlock();
    }

    private CFile() {
    }

    public static CFile createNew() {
        return new CFile();
    }

    void addBlock(CFileBlock block) {
        blocks.add(block);
        metaBlock.uid_block.addUID(block.uid);
    }

    void insertBlock(CFileBlock block, int index) {
        blocks.add(index, block);
    }

    void removeBlock(int index) {
        blocks.remove(index);
    }
}
