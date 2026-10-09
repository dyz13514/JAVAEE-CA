package com.group5.cats.dto;

/** Search and paging values submitted by the application list forms. */
public class ApplicationSearch {
    private String keyword = "";
    private int page = 1;
    private int size = 10;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) {
        this.keyword = keyword == null ? "" : keyword.trim();
    }
    public int getPage() { return page; }
    public void setPage(int page) {
        this.page = Math.max(1, Math.min(page, 1000000));
    }
    public int getSize() { return size; }
    public void setSize(int size) {
        this.size = size == 20 || size == 25 ? size : 10;
    }
}
