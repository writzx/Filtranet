package com.writzx.filtranet;

public class BlockHolder {
    public String ip;
    public CBlock block;

    private BlockHolder(String ip, CBlock block) {
        this.ip = ip;
        this.block = block;
    }

    public static BlockHolder of(String ip, CBlock block) {
        return new BlockHolder(ip, block);
    }
}
