package com.group5.cats.dto;

import org.springframework.data.domain.Page;
import com.group5.cats.model.CourseApplication;

/** Converts Spring's zero-based page numbers into labels for the screen. */
public class ApplicationPagination {
    private final Page<CourseApplication> result;

    public ApplicationPagination(Page<CourseApplication> result) {
        this.result = result;
    }

    public int getPage() { return result.getNumber() + 1; }
    public int getTotalPages() { return result.getTotalPages(); }
    public long getTotalElements() { return result.getTotalElements(); }
    public long getFirstResult() {
        return result.isEmpty() ? 0 : (long) result.getNumber() * result.getSize() + 1;
    }
    public long getLastResult() {
        return result.isEmpty() ? 0 : getFirstResult() + result.getNumberOfElements() - 1;
    }
    public int getFirstPageLink() { return Math.max(1, getPage() - 2); }
    public int getLastPageLink() { return Math.min(getTotalPages(), getPage() + 2); }
    public boolean isPrevious() { return result.hasPrevious(); }
    public boolean isNext() { return result.hasNext(); }
}
