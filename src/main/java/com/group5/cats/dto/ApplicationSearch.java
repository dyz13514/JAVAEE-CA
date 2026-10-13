package com.group5.cats.dto;

public class ApplicationSearch {
    private String keyword = "";
    private String reviewState = "all";

    public String getReviewState() { return reviewState; }
    public void setReviewState(String reviewState) {
        this.reviewState = "pending".equals(reviewState) || "processed".equals(reviewState) ? reviewState : "all";
    }
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
