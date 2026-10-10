package com.group5.cats;

import com.group5.cats.model.Category;

public final class CategoryFixtures {
    private CategoryFixtures() {}
    public static Category category(String name) {
        Category category = new Category(name, "Test category", "INTERNAL".equals(name));
        category.setId("INTERNAL".equals(name) ? 1L : "EXTERNAL".equals(name) ? 2L : "CERTIFICATION".equals(name) ? 3L : 4L);
        return category;
    }
}
