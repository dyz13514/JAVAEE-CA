package com.group5.cats.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.service.CourseApplicationService;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.RequestParam;







@Controller
public class EmployeeController {
    private final CourseApplicationService courseApplicationService;
    
    public EmployeeController(CourseApplicationService courseApplicationService) {
        this.courseApplicationService = courseApplicationService;
    }

    @PostMapping("/employee/apply")
    public String submitApplication(@ModelAttribute CourseApplication application, HttpSession session, RedirectAttributes  redirectAttrs) {
        Employee employee = (Employee) session.getAttribute("loggedInUser");
        if (employee == null) {
            return "redirect:/employee/login";
        }
        courseApplicationService.submitApplication(application, employee);
         redirectAttrs.addFlashAttribute("successMessage",
                "Course application submitted successfully!");
        return "redirect:/employee/home";
    }



    @GetMapping ("/employee/home")
    public String showEmployeeHome() {
        return "employee-home";
    }
    @GetMapping("/employee/apply")
    public String showEmployeeApply(Model model) {
        model.addAttribute("application", new CourseApplication());
        return "apply-course";
    }
    
   @GetMapping("/employee/history")
   public String shwMyHistory(HttpSession session, Model model) {
        Employee employee = (Employee) session.getAttribute("loggedInUser");
    if (employee == null) {
        return "redirect:/employee/login";
    }
    model.addAttribute("applications",
            courseApplicationService.findApplicationsByEmployee(employee));
            return "my-history";
   }

   @GetMapping("/employee/history/{id}")
   public String showApplicationDetial(@PathVariable Long id, Model model,HttpServletResponse response) {
     Optional<CourseApplication> result = courseApplicationService.findApplicationById(id);
     if (result.isEmpty()) {
        response.setStatus(404);
        return "error/404";
    }

     model.addAttribute("courseApplication", result.get());
    return "application-detail";
    
    
   }
   
    
    

}
