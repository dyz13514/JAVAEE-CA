package com.group5.cats.controller;

import com.group5.cats.model.Category;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.CategoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {
    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @InitBinder("category")
    public void configureBinding(WebDataBinder binder) {
        binder.setAllowedFields("name", "description", "halfDayAllowed");
    }

    private boolean isLoggedInAdmin(HttpSession session) {
        Employee employee = (Employee) session.getAttribute("loggedInUser");
        if (employee == null) return false;
        if (employee.getRole() != EmployeeRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required.");
        }
        return true;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        if (!isLoggedInAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("categories", categoryService.findAllCategories());
        return "admin/categories";
    }

    @GetMapping("/new")
    public String createForm(HttpSession session, Model model) {
        if (!isLoggedInAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("category", new Category());
        return prepareForm(null, model);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!isLoggedInAdmin(session)) return "redirect:/admin/login";
        Category category = categoryService.findCategoryById(id);
        if (category == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found.");
        model.addAttribute("category", category);
        return prepareForm(id, model);
    }

    @PostMapping("/save")
    public String create(@ModelAttribute("category") Category category, BindingResult result,
            HttpSession session, Model model, RedirectAttributes redirect) {
        if (!isLoggedInAdmin(session)) return "redirect:/admin/login";
        String error = result.hasErrors() ? "Please enter valid category information."
                : categoryService.createCategory(category.getName(), category.getDescription(), category.isHalfDayAllowed());
        if (error != null) {
            model.addAttribute("errorMessage", error);
            return prepareForm(null, model);
        }
        redirect.addFlashAttribute("successMessage", "Category created.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @ModelAttribute("category") Category category,
            BindingResult result, HttpSession session, Model model, RedirectAttributes redirect) {
        if (!isLoggedInAdmin(session)) return "redirect:/admin/login";
        String error = result.hasErrors() ? "Please enter valid category information."
                : categoryService.updateCategory(id, category.getName(), category.getDescription(), category.isHalfDayAllowed());
        if (error != null) {
            model.addAttribute("errorMessage", error);
            return prepareForm(id, model);
        }
        redirect.addFlashAttribute("successMessage", "Category updated.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        if (!isLoggedInAdmin(session)) return "redirect:/admin/login";
        String error = categoryService.deleteCategory(id);
        redirect.addFlashAttribute(error == null ? "successMessage" : "errorMessage",
                error == null ? "Category deleted." : error);
        return "redirect:/admin/categories";
    }

    private String prepareForm(Long id, Model model) {
        Category existing = id == null ? null : categoryService.findCategoryById(id);
        boolean fixed = existing != null && ("INTERNAL".equals(existing.getName())
                || "EXTERNAL".equals(existing.getName()) || "CERTIFICATION".equals(existing.getName()));
        model.addAttribute("fixedCategory", fixed);
        model.addAttribute("fixedHalfDay", fixed && existing.isHalfDayAllowed());
        model.addAttribute("formAction", id == null ? "/admin/categories/save" : "/admin/categories/" + id + "/update");
        model.addAttribute("editing", id != null);
        return "admin/category-form";
    }
}
