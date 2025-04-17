package com.giosoft.lectorpdf.model;

public class PdfItem {
    private String path;
    private String name;
    private long timestamp;

    public PdfItem(String path, String name, long timestamp) {
        this.path = path;
        this.name = name;
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public long getTimestamp() {
        return timestamp;
    }
}


