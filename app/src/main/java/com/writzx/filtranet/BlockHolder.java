package com.writzx.filtranet;

public class BlockHolder {
    public String dest_ip;
    public CBlock block;

    public BlockHolder(String dest_ip, CBlock block) {
        this.dest_ip = dest_ip;
        this.block = block;
    }
}
