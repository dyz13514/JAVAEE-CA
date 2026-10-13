package com.group5.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.group5.cats.model.CommonCourse;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.CommonCourseService;
import com.group5.cats.service.CourseService;
import jakarta.servlet.http.HttpSession;

@Controller
public class AdminCommonCourseController {
    private final CommonCourseService commonCourseService;
    private final CourseService courseService;

    public AdminCommonCourseController(CommonCourseService commonCourseService, CourseService courseService) {
        this.commonCourseService = commonCourseService;
        this.courseService = courseService;
    }

    private boolean isLoggedInAdmin(HttpSession session) {
        Employee employee = (Employee) session.getAttribute("loggedInUser");
        if (employee == null) return false;
        if (employee.getRole() != EmployeeRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required.");
        }
        return true;
    }

    @GetMapping("/admin/common-courses")
    public String showCommonCourses(HttpSession session, Model model) {
        if (!isLoggedInAdmin(session)) return "redirect:/admin/login";
        var commonCourses = commonCourseService.findAllCommonCourses();
        model.addAttribute("commonCourses", commonCourses);
        model.addAttribute("commonCourseIds", commonCourses.stream().map(CommonCourse::getCourse)
                .map(course -> course.getId()).toList());
        model.addAttribute("courses", courseService.findCourses(null, null, null));
        return "admin/common-courses";
    }

    @PostMapping("/admin/common-courses/{courseId}/add")
    public String addCommonCourse(@PathVariable Long courseId, HttpSession session, RedirectAttributes redirect) {
        if (!isLoggedInAdmin(session)) return "redirect:/admin/login";
        String error = commonCourseService.addCommonCourse(courseId);
        redirect.addFlashAttribute(error == null ? "successMessage" : "errorMessage",
                error == null ? "Course added to the common catalogue." : error);
        return "redirect:/admin/common-courses";
    }

    @PostMapping("/admin/common-courses/{id}/delete")
    public String removeCommonCourse(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        if (!isLoggedInAdmin(session)) return "redirect:/admin/login";
        String error = commonCourseService.deleteCommonCourse(id);
        redirect.addFlashAttribute(error == null ? "successMessage" : "errorMessage",
                error == null ? "Course removed from the common catalogue." : error);
        return "redirect:/admin/common-courses";
    }

    @GetMapping({"/admin/common-courses/new", "/admin/common-courses/{id}/edit"})
    public String oldCourseForm(HttpSession session) {
        if (!isLoggedInAdmin(session)) return "redirect:/admin/login";
        return "redirect:/admin/common-courses";
    }
}
