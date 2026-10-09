package com.group5.cats.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import com.group5.cats.service.CourseScheduleService;
import java.util.Optional;
import java.util.List;
import com.group5.cats.model.Course;
import com.group5.cats.service.CourseService;
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

@Controller
public class EmployeeController {

    private final CourseApplicationService courseApplicationService;
    private final EntitlementService entitlementService;
    private final CourseService courseService;
    private final CourseScheduleService scheduleService;

    public EmployeeController(
            CourseApplicationService courseApplicationService,
            EntitlementService entitlementService, CourseService courseService,
            CourseScheduleService scheduleService) {

        this.courseApplicationService = courseApplicationService;
        this.entitlementService = entitlementService;
        this.courseService = courseService;
        this.scheduleService = scheduleService;
    }

    @ModelAttribute("courses")
    public List<Course> availableCourses() {
        return courseService.findAllCourses();
    }

    @InitBinder("courseApplication")
    public void configureApplicationBinding(WebDataBinder binder) {
        binder.setAllowedFields("courseId", "courseTitle", "category", "provider", "fromDate", "toDate",
                "fee", "justification", "dissemination", "halfDay");
    }

    @PostMapping("/employee/apply")
    public String submitApplication(
            @ModelAttribute("courseApplication") CourseApplication application,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttrs) {

        Employee employee =
                (Employee) session.getAttribute("loggedInUser");

        if (employee == null) {
            return "redirect:/employee/login";
        }

        if (application.getCourseId() == null) {
            redirectAttrs.addFlashAttribute("errorMessage", "Please select a course from All courses.");
            return "redirect:/courses";
        }
        String error = bindingResult.hasErrors()
                ? "Please enter valid course dates, fee and half-day selection."
                : courseApplicationService.submitApplication(
                application,
                employee
        );

        if (error != null) {
            addSchedule(application, model);
            model.addAttribute("errorMessage", error);
            model.addAttribute("formAction", "/employee/apply");
            return application.getCourseId() == null ? "legacy-apply-course" : "apply-course";
        }

        redirectAttrs.addFlashAttribute(
                "successMessage",
                "Course application submitted successfully! Training days: "
                        + application.getTrainingDays()
        );

        return "redirect:/employee/home";
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
            @RequestParam(value = "courseId", required = false) Long courseId,
            HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Employee employee = (Employee) session.getAttribute("loggedInUser");
        if (employee == null) {
            return "redirect:/employee/login";
        }
        if (courseId == null) return "redirect:/courses";
        CourseApplication application = new CourseApplication();
        if (courseId != null) {
            Course course = courseService.findCourseById(courseId);
            if (course == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Selected course no longer exists.");
                return "redirect:/courses";
            }
            application.setCourseId(course.getId());
            application.setCourseTitle(course.getTitle());
            application.setCategory(course.getCategory());
            application.setProvider(course.getProvider().getName());
            application.setFee(course.getFee());
        }
        addSchedule(application, model);
        model.addAttribute("courseApplication", application);
        model.addAttribute("formAction", "/employee/apply");
        return "apply-course";
    }

    @GetMapping("/employee/history")
    public String shwMyHistory(HttpSession session, Model model) {

        Employee employee =
                (Employee) session.getAttribute("loggedInUser");

        if (employee == null) {
            return "redirect:/employee/login";
        }

        model.addAttribute(
                "applications",
                courseApplicationService.findApplicationsByEmployee(
                        employee
                )
        );

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
        if (!result.get().getEmployee().getId().equals(viewer.getId())) {
            response.setStatus(403);
            return "error/404";
        }
        addSchedule(result.get(), model);
        model.addAttribute("courseApplication", result.get());

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
        addSchedule(result.get(), model);
        model.addAttribute("courseApplication", result.get());
        model.addAttribute(
                "formAction",
                "/employee/history/" + id + "/edit"
        );

        return result.get().getCourseId() == null ? "legacy-apply-course" : "apply-course";
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
            addSchedule(updatedData, model);
            model.addAttribute("errorMessage", error);
            model.addAttribute("formAction", "/employee/history/" + id + "/edit");
            return updatedData.getCourseId() == null ? "legacy-apply-course" : "apply-course";
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
    private void addSchedule(CourseApplication application, Model model) {
        if (application.getCourseId() == null) return;
        Course course = courseService.findCourseById(application.getCourseId());
        if (course == null) return;
        if (application.getCourseTitle() == null) application.setCourseTitle(course.getTitle());
        if (application.getProvider() == null) application.setProvider(course.getProvider().getName());
        Map<LocalDate, LocalDate> dates = new LinkedHashMap<>(scheduleService.findAvailableDates(course));
        if (application.getId() != null && application.getFromDate() != null && application.getToDate() != null) {
            dates.put(application.getFromDate(), application.getToDate());
        }
        model.addAttribute("selectedCourse", course);
        model.addAttribute("availableDates", dates);
    }

}
