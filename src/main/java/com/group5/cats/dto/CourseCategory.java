package com.group5.cats.dto;

/**
 * Names of the three original categories and the manager report's ALL option.
 * The Category table supplies the selectable categories.
 */
public final class CourseCategory {

    public static final String ALL = "ALL";
    public static final String INTERNAL = "INTERNAL";
    public static final String EXTERNAL = "EXTERNAL";
    public static final String CERTIFICATION = "CERTIFICATION";

    private CourseCategory() {
    }

    public static boolean isBudgetRelevant(String category) {
        return category != null && !category.isBlank() && !INTERNAL.equals(category);
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
