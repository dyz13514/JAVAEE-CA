package com.group5.cats.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.service.CourseApplicationService;



@Controller
public class EmployeeController {
    private final CourseApplicationService courseApplicationService;
    
    public EmployeeController(CourseApplicationService courseApplicationService) {
        this.courseApplicationService = courseApplicationService;
    }

    @PostMapping("/employee/apply")
    public String submitApplication(@ModelAttribute CourseApplication application, HttpSession session, RedirectAttributes redirectAttributes) {
        Employee employee = (Employee) session.getAttribute("loggedInUser");
        if (employee == null) {
            return "redirect:/employee/login";
        }
        courseApplicationService.submitApplication(application, employee);
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
    

}
