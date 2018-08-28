package com.writzx.filtranet;

public class ProgressListItem implements ListItem {
    private String filename;
    private String filepath;

    private String status;
    private int bytesRead;
    private int totalLength;

    public ProgressListItem(String filename, String filepath) {
        this.filename = filename;
        this.filepath = filepath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getBytesRead() {
        return bytesRead;
    }

    public void setBytesRead(int bytesRead) {
        this.bytesRead = bytesRead;
    }

    public int getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(int totalLength) {
        this.totalLength = totalLength;
    }

    public String getFileName() {
        return filename;
    }

    public String getFilePath() {
        return filepath;
    }

    @Override
    public int getType() {
        return ListItem.TYPE_PROGRESS;
    }
}
