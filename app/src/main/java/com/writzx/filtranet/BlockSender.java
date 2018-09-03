package com.writzx.filtranet;

import com.google.common.primitives.Shorts;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockSender implements Runnable {
    private Thread thread;
    private final LinkedBlockingQueue<BlockHolder> queue;
    // do not need ip for this since will be sent to any ip address which requests the block
    private final TreeSet<Short> blockUIDs;
    public final LinkedBlockingQueue<BlockHolder> localBlocks = new LinkedBlockingQueue<>();

    private boolean started = false;

    public BlockSender() {
        this.queue = new LinkedBlockingQueue<>();
        this.blockUIDs = new TreeSet<>();
        thread = new Thread(this);
    }

    @Override
    public void run() {
        while (started) {
            try {
                send(queue.take());
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void send(BlockHolder b) throws IOException {
        UDPSender.getInstance().send(b);
    }

    public void start() {
        started = true;
        thread.start();
    }

    public void stop() {
        started = false;
    }

    public void queueFile(String ip, CFile file) throws InterruptedException, IOException {
        // only add the meta block and wait for requests
        queue.put(BlockHolder.of(ip, file.metaBlock));

        blockUIDs.add(file.metaBlock.attached_uid);

        // do recursive add of block uids
        CUIDBlock ublk = file.metaBlock.uid_block != null ? file.metaBlock.uid_block : CUIDBlock.open(MainActivity.sendCache, file.metaBlock.attached_uid);

        do {
            CUIDBlock.save(ublk, MainActivity.sendCache);
            blockUIDs.add(ublk.uid);
        } while ((ublk = ublk.next) != null);

        for (CFileBlock block : file.blocks) {
            blockUIDs.add(block.uid);
        }
    }

    public void queueBlock(BlockHolder bh) throws InterruptedException, IOException {
        if (bh.block.b_type == CBlockType.UID) {
            queueBlock(bh.ip, (CUIDBlock) bh.block);
            return;
        }
        queue.put(BlockHolder.of(bh.ip, bh.block));
    }

    public void queueBlock(String ip, CUIDBlock block) throws InterruptedException, IOException {
        queue.put(BlockHolder.of(ip, block));

        if (block.next != null && block.next_uid != block.uid) {
            CUIDBlock.save(block.next, MainActivity.sendCache);
        }
    }

    public void queueUIDBlock(String ip, short uid) throws IOException, InterruptedException {
        try {
            if (!blockUIDs.contains(uid)) {
                respondBlockFail(ip, uid);
                return;
            }
            CUIDBlock blk = CUIDBlock.open(MainActivity.sendCache, uid);
            queue.put(BlockHolder.of(ip, blk));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            respondBlockFail(ip, uid);
        }
    }

    public void requestFileBlock(String ip, boolean nack, short... uids) {
        if (nack) {
            CInfoBlock infBlock = new CInfoBlock();
            infBlock.info_code = CInfoBlock.INFO_NACK;
            infBlock.uids = uids;

            infBlock.message = "CFileBlock";

            while (true) {
                try {
                    queueBlock(BlockHolder.of(ip, infBlock));
                    break;
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            List<Short> uid_s = Shorts.asList(uids);
            for (short uid : uids) {
                CBlock b = getLocal(uid, CBlockType.File);
                if (b == null) {
                    uid_s.add(uid);
                } else {
                    while (true) {
                        try {
                            localBlocks.put(BlockHolder.of(ip, b));
                            break;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            CInfoBlock infBlock = new CInfoBlock();
            infBlock.info_code = CInfoBlock.INFO_ACK;
            infBlock.uids = Shorts.toArray(uid_s);

            infBlock.message = "CFileBlock";

            while (true) {
                try {
                    queueBlock(BlockHolder.of(ip, infBlock));
                    break;
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void requestUIDBlock(String ip, short... uids) {
        while (true) {
            List<Short> uid_s = Shorts.asList(uids);
            for (short uid : uids) {
                CBlock b = getLocal(uid, CBlockType.UID);
                if (b == null) {
                    uid_s.add(uid);
                } else {
                    while (true) {
                        try {
                            localBlocks.put(BlockHolder.of(ip, b));
                            break;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            CInfoBlock infBlock = new CInfoBlock();
            infBlock.info_code = CInfoBlock.INFO_ACK;
            infBlock.uids = Shorts.toArray(uid_s);

            infBlock.message = "CUIDBlock";

            while (true) {
                try {
                    queueBlock(BlockHolder.of(ip, infBlock));
                    break;
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void respondBlockFail(String ip, short... uid) throws IOException, InterruptedException {
        CInfoBlock infBlock = new CInfoBlock();
        infBlock.info_code = CInfoBlock.INFO_NACK;
        infBlock.uids = uid;

        infBlock.message = "BlockFail";

        queueBlock(BlockHolder.of(ip, infBlock));
    }

    public CBlock getLocal(short uid, CBlockType type) {
        File f = new File(MainActivity.receiveCache, uid + "");
        try (FileInputStream fis = new FileInputStream(f); DataInputStream din = new DataInputStream(fis)) {
            CBlock blk = CBlock.factory(din);

            if (blk.b_type != type) {
                f.delete();
                return null;
            }

            blk.read(din);

            return blk;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
