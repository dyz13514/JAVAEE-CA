package com.group5.cats.dto;

import java.util.List;

/**
 * Course categories stored on course applications, plus the filter value used by
 * manager reports. Labels match the wording already used across the portal.
 */
public final class CourseCategory {

    public static final String ALL = "ALL";
    public static final String INTERNAL = "INTERNAL";
    public static final String EXTERNAL = "EXTERNAL";
    public static final String CERTIFICATION = "CERTIFICATION";

    private CourseCategory() {
    }

    /** Filter values: all categories first, then the concrete categories. */
    public static List<String> filterValues() {
        return List.of(ALL, INTERNAL, EXTERNAL, CERTIFICATION);
    }

    public static boolean isValidFilter(String category) {
        return filterValues().contains(category);
    }

    public static boolean isBudgetRelevant(String category) {
        return EXTERNAL.equals(category) || CERTIFICATION.equals(category);
    }

    public static String label(String category) {
        if (INTERNAL.equals(category)) {
            return "Internal training";
        }
        if (EXTERNAL.equals(category)) {
            return "External course";
        }
        if (CERTIFICATION.equals(category)) {
            return "Professional certification";
        }
        return category == null ? "" : category;
    }
}
