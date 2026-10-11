package com.group5.cats.controller;

import org.springframework.data.domain.Page;
import com.group5.cats.dto.ApplicationSearch;
import com.group5.cats.dto.ApplicationPagination;
import com.group5.cats.service.CategoryService;
import com.group5.cats.service.TrainingProviderService;
import com.group5.cats.service.CommonCourseService;
import com.group5.cats.service.CourseService;
import com.group5.cats.dto.CourseOption;
import java.util.Optional;
import java.util.List;
import com.group5.cats.model.CommonCourse;
import com.group5.cats.model.Category;
import com.group5.cats.model.TrainingProvider;
import java.time.LocalDate;
import java.util.Comparator;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.CourseApplicationService;
import com.group5.cats.service.EntitlementService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class EmployeeController {

    private final CourseApplicationService courseApplicationService;
    private final EntitlementService entitlementService;
    private final CommonCourseService commonCourseService;
    private final CategoryService categoryService;
    private final TrainingProviderService providerService;
    private final CourseService courseService;

    public EmployeeController(CourseApplicationService courseApplicationService,
            EntitlementService entitlementService, CommonCourseService commonCourseService,
            CategoryService categoryService, TrainingProviderService providerService, CourseService courseService) {
        this.courseApplicationService = courseApplicationService;
        this.entitlementService = entitlementService;
        this.commonCourseService = commonCourseService;
        this.categoryService = categoryService;
        this.providerService = providerService;
        this.courseService = courseService;
    }

    @ModelAttribute("courseOptions")
    public List<CourseOption> availableCourses() {
        return courseService.findAvailableCourses();
    }

    @ModelAttribute("categories")
    public List<Category> availableCategories() {
        return categoryService.findAllCategories();
    }

    @ModelAttribute("providers")
    public List<TrainingProvider> availableProviders() {
        return providerService.findAllProviders();
    }

    @InitBinder("courseApplication")
    public void configureApplicationBinding(WebDataBinder binder) {
        binder.setAllowedFields("courseId", "courseTitle", "categoryId", "providerId", "fromDate", "toDate",
                "justification", "dissemination", "halfDay");
    }

    @PostMapping("/employee/apply")
    public String submitApplication(
            @ModelAttribute("courseApplication") CourseApplication application,
            BindingResult bindingResult,
            @RequestParam(value = "lockedCourseId", required = false) Long lockedCourseId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttrs) {

        Employee employee =
                (Employee) session.getAttribute("loggedInUser");

        if (employee == null) {
            return "redirect:/employee/login";
        }

        if (employee.getRole() == EmployeeRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This role cannot use this application endpoint.");
        }
        if (lockedCourseId != null) {
            application.setCourseId(lockedCourseId);
            model.addAttribute("courseLocked", true);
            model.addAttribute("lockedCourseId", lockedCourseId);
        }
        String error = bindingResult.hasErrors()
                ? "Please enter valid course dates, fee and half-day selection."
                : courseApplicationService.submitApplication(
                application,
                employee
        );

        if (error != null) {
            model.addAttribute("errorMessage", error);
            model.addAttribute("formAction", "/employee/apply");
            return "apply-course";
        }

        redirectAttrs.addFlashAttribute(
                "successMessage",
                "Course application submitted successfully! Training days: "
                        + application.getTrainingDays()
        );

        return "redirect:/employee/history";
    }

    @GetMapping("/employee/home")
    public String showEmployeeHome(HttpSession session, Model model) {

        Employee employee =
                (Employee) session.getAttribute("loggedInUser");

        if (employee == null) {
            return "redirect:/employee/login";
        }

        if (employee.getRole() == EmployeeRole.ADMIN) {
            return "redirect:/admin/home";
        }

        int year = LocalDate.now().getYear();
        model.addAttribute("entitlementYear", year);
        model.addAttribute("allowance",
                entitlementService.getEntitlementSummary(employee.getId(), year).orElse(null));
        model.addAttribute("recentApplications",
                courseApplicationService.findApplicationsByEmployee(employee).stream()
                        .sorted(Comparator.comparing(CourseApplication::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(3).toList());
        return "employee-home";
    }

    @GetMapping("/employee/apply")
    public String showEmployeeApply(
            @RequestParam(value = "commonCourseId", required = false) Long commonCourseId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Employee employee = (Employee) session.getAttribute("loggedInUser");
        if (employee == null) {
            return "redirect:/employee/login";
        }
        if (employee.getRole() == EmployeeRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrators manage courses but cannot apply.");
        }
        CourseApplication application = new CourseApplication();
        if (courseId != null) {
            var course = courseService.findCourseById(courseId).orElse(null);
            if (course == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Selected course no longer exists.");
                return "redirect:/employee/courses";
            }
            application.setCourse(course);
            application.setCourseTitle(course.getTitle());
            application.setCategory(course.getCategory());
            application.setProvider(course.getProvider());
            application.setFee(course.getFee());
        } else if (commonCourseId != null) {
            CommonCourse course = commonCourseService.findCommonCourseById(commonCourseId);
            if (course == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Selected course no longer exists.");
                return "redirect:/employee/courses";
            }
            application.setCourse(course.getCourse());
            application.setCourseTitle(course.getTitle());
            application.setCategory(course.getCategory());
            application.setProvider(course.getProvider());
            application.setFee(course.getFee());
        }
        model.addAttribute("courseApplication", application);
        if (courseId != null || commonCourseId != null) {
            model.addAttribute("courseLocked", true);
            model.addAttribute("lockedCourseId", application.getCourseId());
        }
        model.addAttribute("formAction", "/employee/apply");
        return "apply-course";
    }

    @GetMapping("/employee/history")
    public String showMyHistory(@ModelAttribute("search") ApplicationSearch search,
            HttpSession session, Model model) {

        Employee employee =
                (Employee) session.getAttribute("loggedInUser");

        if (employee == null) {
            return "redirect:/employee/login";
        }

        Page<CourseApplication> result = courseApplicationService.searchEmployeeApplications(employee, true, search);
        model.addAttribute("applications", result.getContent());
        model.addAttribute("pagination", new ApplicationPagination(result));
        model.addAttribute("listUrl", "/employee/history");

        return "my-history";
    }

    @GetMapping("/employee/history/{id}")
    public String showApplicationDetial(
            @PathVariable Long id,
            Model model,
            HttpServletResponse response, HttpSession session) {

        Optional<CourseApplication> result =
                courseApplicationService.findApplicationById(id);

        if (result.isEmpty()) {
            response.setStatus(404);

            return "error/404";
        }

        Employee viewer = (Employee) session.getAttribute("loggedInUser");
        if (viewer == null) return "redirect:/employee/login";
        Employee applicant = result.get().getEmployee();
        boolean isOwner = viewer.getId().equals(applicant.getId());
        // The assigned manager may also open a subordinate's application to
        // review the complete details before deciding (spec, page 7).
        boolean isSupervisor = viewer.getRole() == EmployeeRole.MANAGER
                && applicant.getSupervisor() != null
                && viewer.getId().equals(applicant.getSupervisor().getId());
        if (!isOwner && !isSupervisor) {
            response.setStatus(403);
            return "error/404";
        }
        model.addAttribute("courseApplication", result.get());
        model.addAttribute("isOwner", isOwner);

        return "application-detail";
    }

    @PostMapping("/employee/history/{id}/withdraw")
    public String withdrawApplication(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        Employee employee =
                (Employee) session.getAttribute("loggedInUser");

        if (employee == null) {
            return "redirect:/employee/login";
        }

        String message = courseApplicationService.withdrawApplication(
                id,
                employee
        );

        redirectAttrs.addFlashAttribute("message", message);

        return "redirect:/employee/history";
    }

    @GetMapping("/employee/history/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            HttpSession session,
            HttpServletResponse response) {

        Employee employee =
                (Employee) session.getAttribute("loggedInUser");

        if (employee == null) {
            return "redirect:/employee/login";
        }

        Optional<CourseApplication> result =
                courseApplicationService.findApplicationById(id);

        if (result.isEmpty()) {
            response.setStatus(404);

            return "error/404";
        }

        Employee viewer = (Employee) session.getAttribute("loggedInUser");
        if (viewer == null) return "redirect:/employee/login";
        if (!result.get().getEmployee().getId().equals(viewer.getId())) {
            response.setStatus(403);
            return "error/404";
        }
        model.addAttribute("courseApplication", result.get());
        model.addAttribute(
                "formAction",
                "/employee/history/" + id + "/edit"
        );

        return "apply-course";
    }

    @PostMapping("/employee/history/{id}/edit")
    public String updateAppliaction(
            @PathVariable Long id,
            @ModelAttribute("courseApplication") CourseApplication updatedData,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttrs) {

        Employee employee =
                (Employee) session.getAttribute("loggedInUser");

        if (employee == null) {
            return "redirect:/employee/login";
        }

        String error = bindingResult.hasErrors()
                ? "Please enter valid course dates, fee and half-day selection."
                : courseApplicationService.updateApplication(
                id,
                updatedData,
                employee
        );

        if (error != null) {
            updatedData.setId(id);
            model.addAttribute("errorMessage", error);
            model.addAttribute("formAction", "/employee/history/" + id + "/edit");
            return "apply-course";
        }

        redirectAttrs.addFlashAttribute(
                "message",
                "Course application updated successfully"
        );

        return "redirect:/employee/history";
    }

    @PostMapping("/employee/history/{id}/cancel")
    public String cancelApplication(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        Employee employee =
                (Employee) session.getAttribute("loggedInUser");

        if (employee == null) {
            return "redirect:/employee/login";
        }

        String message = courseApplicationService.cancelApplication(
                id,
                employee
        );

        redirectAttrs.addFlashAttribute("message", message);

        return "redirect:/employee/history";
    }

    @PostMapping("/employee/history/{id}/complete")
    public String completeApplication(
            @PathVariable Long id,
            @RequestParam String experienceComments,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        Employee employee =
                (Employee) session.getAttribute("loggedInUser");

        if (employee == null) {
            return "redirect:/employee/login";
        }

        String message = courseApplicationService.completeApplication(
                id,
                employee,
                experienceComments
        );

        redirectAttrs.addFlashAttribute("message", message);

        return "redirect:/employee/history";
    }

}
