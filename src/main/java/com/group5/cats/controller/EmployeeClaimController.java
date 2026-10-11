package com.group5.cats.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.cats.dto.CourseFeeClaimForm;
import com.group5.cats.model.ClaimStatus;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.CourseFeeClaim;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.service.CourseFeeClaimService;

import jakarta.servlet.http.HttpSession;

@Controller
public class EmployeeClaimController {

    private final CourseFeeClaimService claimService;
    private final EmployeeRepository employeeRepository;

    public EmployeeClaimController(CourseFeeClaimService claimService, EmployeeRepository employeeRepository) {
        this.claimService = claimService;
        this.employeeRepository = employeeRepository;
    }

    @InitBinder("claimForm")
    public void bindClaimForm(WebDataBinder binder) {
        binder.setAllowedFields("applicationId", "version", "amount", "paidPersonally");
    }

    @GetMapping("/employee/claims")
    public String showMyClaims(HttpSession session, Model model) {
        Employee employee = getCurrentUser(session);
        if (employee == null) {
            return "redirect:/employee/login";
        }
        if (employee.getRole() == EmployeeRole.ADMIN) {
            return "redirect:/admin/claims";
        }
        model.addAttribute("claims", claimService.findMyClaims(employee));
        model.addAttribute("pageTitle", "My claims");
        model.addAttribute("activeNavigation", "claims");
        model.addAttribute("canSubmit", true);
        return "claims/list";
    }

    @GetMapping("/employee/claims/new")
    public String showNewClaim(@RequestParam(required = false) Long applicationId,
            HttpSession session, Model model) {
        Employee employee = getCurrentUser(session);
        if (employee == null) {
            return "redirect:/employee/login";
        }
        if (employee.getRole() == EmployeeRole.ADMIN) {
            return "redirect:/admin/claims";
        }

        List<CourseFeeClaim> existingClaims = claimService.findMyClaims(employee);
        for (CourseFeeClaim existingClaim : existingClaims) {
            if (existingClaim.getApplication().getId().equals(applicationId)) {
                return "redirect:/claims/" + existingClaim.getId();
            }
        }
        CourseFeeClaimForm form = new CourseFeeClaimForm();
        List<CourseApplication> applications = claimService.findEligibleApplications(employee);
        for (CourseApplication application : applications) {
            if (application.getId().equals(applicationId)) {
                form.setApplicationId(applicationId);
                form.setAmount(BigDecimal.valueOf(application.getFee()));
                break;
            }
        }
        model.addAttribute("claimForm", form);
        prepareForm(model, applications, null);
        return "claims/form";
    }

    @GetMapping("/employee/claims/{id}/edit")
    public String showResubmitClaim(@PathVariable Long id, HttpSession session, Model model) {
        Employee employee = getCurrentUser(session);
        if (employee == null) {
            return "redirect:/employee/login";
        }
        CourseFeeClaim claim = findOwnedRejectedClaim(id, employee);
        CourseFeeClaimForm form = new CourseFeeClaimForm();
        form.setApplicationId(claim.getApplication().getId());
        form.setVersion(claim.getVersion());
        form.setAmount(claim.getAmount());
        form.setPaidPersonally(claim.isPaidPersonally());
        model.addAttribute("claimForm", form);
        prepareForm(model, List.of(claim.getApplication()), claim);
        return "claims/form";
    }

    @PostMapping("/employee/claims/save")
    public String saveNewClaim(@ModelAttribute("claimForm") CourseFeeClaimForm form,
            BindingResult bindingResult, @RequestParam(required = false) MultipartFile receipt,
            @RequestParam(required = false) MultipartFile certificate,
            HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        return submitClaim(null, form, bindingResult, receipt, certificate, session, model, redirectAttributes);
    }

    @PostMapping("/employee/claims/{id}/resubmit")
    public String resubmitClaim(@PathVariable Long id, @ModelAttribute("claimForm") CourseFeeClaimForm form,
            BindingResult bindingResult, @RequestParam(required = false) MultipartFile receipt,
            @RequestParam(required = false) MultipartFile certificate,
            HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        return submitClaim(id, form, bindingResult, receipt, certificate, session, model, redirectAttributes);
    }

    private String submitClaim(Long id, CourseFeeClaimForm form, BindingResult bindingResult,
            MultipartFile receipt, MultipartFile certificate, HttpSession session,
            Model model, RedirectAttributes redirectAttributes) {
        Employee employee = getCurrentUser(session);
        if (employee == null) {
            return "redirect:/employee/login";
        }
        if (employee.getRole() == EmployeeRole.ADMIN) {
            return "redirect:/admin/claims";
        }
        CourseFeeClaim existingClaim = null;
        if (id != null) {
            existingClaim = findOwnedRejectedClaim(id, employee);
        }

        String error = null;
        if (bindingResult.hasErrors()) {
            error = "Please select a course and enter a valid claim amount.";
        } else {
            try {
                error = claimService.submitClaim(id, form, receipt, certificate, employee);
            } catch (OptimisticLockingFailureException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", "This claim has changed. Please reload and try again.");
                return "redirect:/employee/claims";
            } catch (DataIntegrityViolationException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", "The claim could not be saved. Check My claims before submitting again.");
                return "redirect:/employee/claims";
            } catch (IllegalStateException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
                return "redirect:/employee/claims";
            } catch (IllegalArgumentException exception) {
                error = exception.getMessage();
            }
        }
        if (error == null) {
            redirectAttributes.addFlashAttribute("successMessage", "Your claim has been submitted for manager approval.");
            return "redirect:/employee/claims";
        }

        List<CourseApplication> applications;
        if (existingClaim == null) {
            applications = claimService.findEligibleApplications(employee);
        } else {
            applications = List.of(existingClaim.getApplication());
        }
        model.addAttribute("errorMessage", error + " Please select any new attachments again.");
        prepareForm(model, applications, existingClaim);
        return "claims/form";
    }

    private void prepareForm(Model model, List<CourseApplication> applications, CourseFeeClaim claim) {
        model.addAttribute("applications", applications);
        model.addAttribute("claim", claim);
        model.addAttribute("resubmission", claim != null);
        if (claim == null) {
            model.addAttribute("formAction", "/employee/claims/save");
        } else {
            model.addAttribute("formAction", "/employee/claims/" + claim.getId() + "/resubmit");
        }
    }

    private CourseFeeClaim findOwnedRejectedClaim(Long id, Employee employee) {
        CourseFeeClaim claim;
        try {
            claim = claimService.findViewableClaim(id, employee);
        } catch (SecurityException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!claim.getApplication().getEmployee().getId().equals(employee.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (claim.getStatus() != ClaimStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only rejected claims can be revised.");
        }
        return claim;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleLargeUpload(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Each attachment must be no larger than 10 MB. Please submit again.");
        return "redirect:/employee/claims";
    }

    private Employee getCurrentUser(HttpSession session) {
        Employee sessionEmployee = (Employee) session.getAttribute("loggedInUser");
        if (sessionEmployee == null || sessionEmployee.getId() == null) {
            return null;
        }
        Optional<Employee> employeeResult = employeeRepository.findById(sessionEmployee.getId());
        if (employeeResult.isEmpty()) {
            return null;
        }
        return employeeResult.get();
    }
}
