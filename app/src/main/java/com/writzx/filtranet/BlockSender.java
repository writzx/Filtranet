package com.writzx.filtranet;

import com.google.common.primitives.Ints;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockSender implements Runnable {
    private Thread thread;

    private final LinkedBlockingQueue<BlockHolder> queue;
    private final List<CFileBlock> fileBlocks;

    public final LinkedBlockingQueue<BlockHolder> localBlocks = new LinkedBlockingQueue<>();

    private boolean started = false;

    public BlockSender() {
        this.queue = new LinkedBlockingQueue<>();
        this.fileBlocks = new ArrayList<>();
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

        fileBlocks.addAll(file.blocks);
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

    public void queueUIDBlock(String ip, int uid) throws IOException, InterruptedException {
        try {
            File uid_file = new File(MainActivity.sendCache, Utils.toHex(uid));
            if (!uid_file.exists()) {
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

    public void queueFileBlock(String ip, int uid) throws IOException, InterruptedException {
        CFileBlock bk = findFileBlock(uid);
        if (bk == null) {
            respondBlockFail(ip, uid);
            return;
        }

        queue.put(BlockHolder.of(ip, bk));
    }

    public CFileBlock findFileBlock(int uid) {
        for (CFileBlock blk : fileBlocks) {
            if (blk.valid && blk.uid == uid) return blk;
        }
        return null;
    }

    public void requestFileBlock(String ip, boolean nack, int... uids) throws IOException, InterruptedException {
        if (nack) {
            CInfoBlock infBlock = new CInfoBlock();
            infBlock.info_code = CInfoBlock.INFO_NACK;
            infBlock.uids = uids;

            infBlock.message = "CFileBlock";

            queueBlock(BlockHolder.of(ip, infBlock));

        } else {
            List<Integer> uid_s = new ArrayList<>(Ints.asList(uids));
            for (int uid : uids) {
                CBlock b = getLocal(uid, CBlockType.File);
                if (b == null) {
                    uid_s.add(uid);
                } else {
                    localBlocks.put(BlockHolder.of(ip, b));
                }
            }

            CInfoBlock infBlock = new CInfoBlock();
            infBlock.info_code = CInfoBlock.INFO_ACK;
            infBlock.uids = Ints.toArray(uid_s);

            infBlock.message = "CFileBlock";

            queueBlock(BlockHolder.of(ip, infBlock));
        }
    }

    public void requestUIDBlock(String ip, int... uids) throws IOException, InterruptedException {
        List<Integer> uid_s = new ArrayList<>(Ints.asList(uids));
        for (int uid : uids) {
            CBlock b = getLocal(uid, CBlockType.UID);
            if (b != null) {
                localBlocks.put(BlockHolder.of(ip, b));
                uid_s.remove(Integer.valueOf(uid));
            }
        }

        CInfoBlock infBlock = new CInfoBlock();
        infBlock.info_code = CInfoBlock.INFO_ACK;
        infBlock.uids = Ints.toArray(uid_s);

        infBlock.message = "CUIDBlock";

        queueBlock(BlockHolder.of(ip, infBlock));

    }

    public void respondBlockFail(String ip, int... uid) throws IOException, InterruptedException {
        CInfoBlock infBlock = new CInfoBlock();
        infBlock.info_code = CInfoBlock.INFO_NACK;
        infBlock.uids = uid;

        infBlock.message = "BlockFail";

        queueBlock(BlockHolder.of(ip, infBlock));
    }

    public CBlock getLocal(int uid, CBlockType type) {
        File f = new File(MainActivity.receiveCache, Utils.toHex(uid));
        try (FileInputStream fis = new FileInputStream(f); DataInputStream din = new DataInputStream(fis)) {
            CBlock blk = CBlock.factory(din);

            if (blk.b_type != type) {
                f.delete();
                return null;
            }

            blk.read(din);

            return blk;
        } catch (IOException ignored) {
        }
        return null;
    }
}
