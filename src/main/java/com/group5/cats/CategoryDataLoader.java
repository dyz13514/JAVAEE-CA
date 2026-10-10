package com.group5.cats;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.group5.cats.model.Category;
import com.group5.cats.repository.CategoryRepository;

@Component
@Order(0)
public class CategoryDataLoader implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public CategoryDataLoader(
            CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        if (categoryRepository.findByNameIgnoreCase("INTERNAL").isEmpty()) {
            categoryRepository.save(new Category(
                    "INTERNAL",
                    "Internal training provided by the organisation.",
                    true));
        }

        if (categoryRepository.findByNameIgnoreCase("EXTERNAL").isEmpty()) {
            categoryRepository.save(new Category(
                    "EXTERNAL",
                    "External courses provided by training providers.",
                    false));
        }

        if (categoryRepository.findByNameIgnoreCase("CERTIFICATION").isEmpty()) {
            categoryRepository.save(new Category(
                    "CERTIFICATION",
                    "Training for professional certification.",
                    false));
        }
    }
}