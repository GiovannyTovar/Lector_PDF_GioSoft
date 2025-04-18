package com.giosoft.lectorpdf.model;

import java.util.List;

public class PdfGroup {
    private String title;
    private List<PdfItem> items;

    public PdfGroup(String title, List<PdfItem> items) {
        this.title = title;
        this.items = items;
    }

    public String getTitle() {
        return title;
    }

    public List<PdfItem> getItems() {
        return items;
    }
}