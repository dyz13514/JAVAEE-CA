package com.group5.cats.service;

import java.util.List;

import com.group5.cats.model.Category;

public interface CategoryService {

    List<Category> findAllCategories();

    Category findCategoryById(Long id);
    
    String createCategory(
            String name,
            String description,
            boolean halfDayAllowed);
    
    String updateCategory(
            Long id,
            String name,
            String description,
            boolean halfDayAllowed);
    String deleteCategory(Long id);
}
