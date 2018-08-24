package com.writzx.filtranet;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

class CFile {
    CMetaBlock metaBlock;

    List<CFileBlock> blocks;
    FileDescriptor fd;
    // should contain all other attribute data including path

    CFile(FileInputStream file, String filename, String mimeType) throws IOException {
        fd = file.getFD();

        int bytesRead;

        CRC16 crc = new CRC16();

        metaBlock = new CMetaBlock();
        byte[] buffer = new byte[CBlock.BLOCK_LENGTH];

        for (metaBlock.length = 0; (bytesRead = file.read(buffer, 0, CBlock.BLOCK_LENGTH)) != -1; metaBlock.length += bytesRead) {
            CFileBlock sblock = new CFileBlock();

            sblock.uid = UID.generate();

            sblock.offset = metaBlock.length;
            sblock.length = bytesRead;

            crc.update(buffer, 0, buffer.length);
            sblock.crc = crc.getShortValue();
            crc.reset();

            sblock.cfile = this;

            blocks.add(sblock);
        }

        this.metaBlock.filename = filename;
        this.metaBlock.mimeType = mimeType;
    }
}
