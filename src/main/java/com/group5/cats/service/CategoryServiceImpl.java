package com.group5.cats.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group5.cats.model.Category;
import com.group5.cats.repository.CategoryRepository;
import com.group5.cats.repository.CommonCourseRepository;
import com.group5.cats.repository.CourseApplicationRepository;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    private final CommonCourseRepository commonCourseRepository;
    private final CourseApplicationRepository applicationRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
            CommonCourseRepository commonCourseRepository, CourseApplicationRepository applicationRepository) {
        this.categoryRepository = categoryRepository;
        this.commonCourseRepository = commonCourseRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public List<Category> findAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Override
    public Category findCategoryById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }
    
    @Override
    public String createCategory(
            String name,
            String description,
            boolean halfDayAllowed) {

        if (name == null || name.isBlank()) {
            return "Category name is required.";
        }

        String categoryName = name.trim();
        if ("ALL".equalsIgnoreCase(categoryName)) {
            return "ALL is reserved for report filters. Please use another category name.";
        }

        if (categoryName.length() > 255) {
            return "Category name must not exceed 255 characters.";
        }


        if (categoryRepository
                .findByNameIgnoreCase(categoryName).isPresent()) {
            return "Category name already exists.";
        }


        String categoryDescription = description;

        if (categoryDescription != null) {
            categoryDescription = categoryDescription.trim();

            if (categoryDescription.length() > 1000) {
                return "Category description must not exceed 1000 characters.";
            }
        }

        Category category = new Category(
                categoryName,
                categoryDescription,
                halfDayAllowed);

        categoryRepository.save(category);

        return null;
    }
    
    @Override
    @Transactional
    public String updateCategory(
            Long id,
            String name,
            String description,
            boolean halfDayAllowed) {

        if (id == null || id <= 0) {
            return "Category ID must be positive.";
        }

        Category category =
                categoryRepository.findById(id).orElse(null);

        if (category == null) {
            return "Category not found.";
        }

        if (name == null || name.isBlank()) {
            return "Category name is required.";
        }

        String categoryName = name.trim();
        if ("ALL".equalsIgnoreCase(categoryName)) {
            return "ALL is reserved for report filters. Please use another category name.";
        }

        if (categoryName.length() > 255) {
            return "Category name must not exceed 255 characters.";
        }

        boolean defaultCategory = isDefaultCategory(category);

        // 原本三种类别不能修改名称。
        if (defaultCategory
                && !category.getName().equals(categoryName)) {
            return "The original three category names cannot be changed.";
        }

        // 原本三种类别不能修改是否允许半天。
        if (defaultCategory
                && category.isHalfDayAllowed() != halfDayAllowed) {
            return "The half-day setting of the original three categories cannot be changed.";
        }

        Category existing = categoryRepository
                .findByNameIgnoreCase(categoryName).orElse(null);

        if (existing != null && !existing.getId().equals(id)) {
            return "Category name already exists.";
        }

        String categoryDescription = description;

        if (categoryDescription != null) {
            categoryDescription = categoryDescription.trim();

            if (categoryDescription.length() > 1000) {
                return "Category description must not exceed 1000 characters.";
            }
        }


        category.setName(categoryName);
        category.setDescription(categoryDescription);
        category.setHalfDayAllowed(halfDayAllowed);

        categoryRepository.save(category);

        return null;
    }
    @Override
    @Transactional
    public String deleteCategory(Long id) {
        if (id == null || id <= 0) return "Category ID must be positive.";
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) return "Category not found.";
        if (isDefaultCategory(category)) return "The original three categories cannot be deleted.";
        if (commonCourseRepository.existsByCategory_Id(id)
                || applicationRepository.existsByCategory_Id(id)) {
            return "This category is in use and cannot be deleted.";
        }
        categoryRepository.delete(category);
        return null;
    }

    private boolean isDefaultCategory(Category category) {
        return "INTERNAL".equals(category.getName()) || "EXTERNAL".equals(category.getName())
                || "CERTIFICATION".equals(category.getName());
    }
}
