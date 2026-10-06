package com.group5.cats.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.cats.model.AnnualEntitlement;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.service.EntitlementService;

import jakarta.servlet.http.HttpSession;

@Controller
public class EntitlementController {
	
	private final EntitlementService entitlementService;
	private final EmployeeRepository employeeRepository;
	
	public EntitlementController (
			EntitlementService entitlementService,
			EmployeeRepository employeeRepository) {
		this.entitlementService = entitlementService;
		this.employeeRepository = employeeRepository;
	}
	
	private Employee getCurrentUser(HttpSession session) {
		
		Employee sessionEmployee = (Employee) session.getAttribute("loggedInUser");
		
		if (sessionEmployee == null) {
			return null;
		}
		
		return employeeRepository.findById(sessionEmployee.getId()).orElse(null);
	}
	
	private List<Employee> getQueryableEmployees(Employee currentUser) {
		
		if(currentUser.getRole() == EmployeeRole.ADMIN) {
			return employeeRepository.findAll();
		}
		
		List<Employee> toBeViewedEmployees = new ArrayList<>();
		toBeViewedEmployees.add(currentUser);
		
		if(currentUser.getRole() == EmployeeRole.MANAGER) {
			toBeViewedEmployees.addAll(employeeRepository.findBySupervisor_Id(currentUser.getId()));
		}
		
		return toBeViewedEmployees;
	}
	//使用List，因为我们这一个方法就可以覆盖三个角色的查询额度。ADMIN查全部，MANAGER和REGULAR_STAFF至少能看到自己，而MANAGER除了自己的还在List加上自己当supervisor的下属
	
	@GetMapping("/entitlements")
	public String showEntitlement(
			@RequestParam(required = false) Long employeeId,
			@RequestParam(required = false) Integer entitlementYear,
			HttpSession session,
			Model model)   //对于URL或其他形式传入参数如abc、超出类型范围的数字等无法转换成 Long、Integer时，Spring 在进入方法前拒绝，通常返回 400，无设置的 message
	{
		
		Employee currentUser = getCurrentUser(session);
		
		if(currentUser == null) {
			return "redirect:/employee/login";
		}
		//未登录，跳转到登录页
		
		List<Employee> toBeViewedEmployees = getQueryableEmployees(currentUser);
		
		Long selectedId;
		if (employeeId == null) {
			selectedId = currentUser.getId();
		} else {
			selectedId = employeeId;
		}
		//已登录且请求未传 employeeId 时，默认查询自己
		
		Integer selectedYear;
		if (entitlementYear == null) {
			selectedYear = LocalDate.now().getYear();
		} else {
			selectedYear = entitlementYear;
		}
		// 已登录且请求未传 entitlementYear 时，默认查询当前年份。
		
		

		model.addAttribute("currentUser", currentUser);
		model.addAttribute("toBeViewedEmployees", toBeViewedEmployees);
		model.addAttribute("selectedId", selectedId);
		model.addAttribute("selectedYear", selectedYear);
		model.addAttribute("canEdit", currentUser.getRole() == EmployeeRole.ADMIN);//传递true或false。html根据true或false检查是否展现编辑div模块
		//向页面传递数据。传递可以被看到的所有toBeViewedEmployees和对应的id，year
		
		Employee selectedEmployee = null;
		
		for(Employee employee : toBeViewedEmployees) {
			if (employee.getId().equals(selectedId)) {
				selectedEmployee = employee;
				break;
			}
		}
		
		if(selectedEmployee == null) {
			model.addAttribute("message", "You are not authorised.");
			return "entitlement";
		}
		//即通过id检查出selectedEmployee不在toBeViewedEmployees里。
		//可能是员工存在但无权查询，也可能没有对应员工
		//零或负数 ID 也会因没有匹配项而在这里被拒绝。
		//总之会直接返回"entitlement"不执行之后的。
		//此时只通过employee.getId()判断员工是否在可被查询的范围。没有验证年份，年份在后面findEntitlement调用到validateInputs验证
		
		
		model.addAttribute("selectedEmployee", selectedEmployee);
		//向页面传递数据。传递被选择查看的selectedEmployee
		
		try{
		Optional<AnnualEntitlement> result = entitlementService.findEntitlement(selectedId, selectedYear);
		
		
			if(result.isPresent()) {
		
			model.addAttribute("entitlement", result.get());
		} else {
			model.addAttribute("message", "No entitlement record for this employee in this year.");
		}
		}
	catch (IllegalArgumentException exception) {
		model.addAttribute("message", exception.getMessage());
	}
		//try-catch 使用到findEntitlement，也就调用到validateInputs，当validateInputs throw出异常由catch接住。
		//前面程序已通过Id判断员工是否在可被查询的范围，这里if-else判断根据年份找没找到记录。
		//没有记录本身不是异常，只显示 message。
		
		
		return "entitlement";
		 
		
	}
	
	@PostMapping("/admin/entitlements")
	public String setEntitlement(
			@RequestParam Long employeeId,
			@RequestParam Integer entitlementYear,
			@RequestParam double trainingDaysLimit,
			@RequestParam double trainingBudget,
			HttpSession session,
			RedirectAttributes redirectAttributes
			) {
		
		Employee currentUser = getCurrentUser(session);
		if (currentUser == null) {
			return "redirect:/employee/login";
		}
		
		if(currentUser.getRole() != EmployeeRole.ADMIN) {
			redirectAttributes.addFlashAttribute("saveMessage", 
					"Only administrators can change entitlements.");
			return "redirect:/entitlements";
		}
		
		try{
			entitlementService.setEntitlement(employeeId, entitlementYear, trainingDaysLimit, trainingBudget);
			redirectAttributes.addFlashAttribute("saveMessage", "Entitlement saved.");
		} catch (IllegalArgumentException exception) {
			redirectAttributes.addFlashAttribute("saveMessage", exception.getMessage());
		}
		//和之前showEntitlement()调用findEntitlement()因此调用validateInputs一样
		//这里setEntitlement因此调用validateInputs，有抛出的exception需要catch
		//使用addFlashAttribute不是addAttribute因为是在"redirect:/entitlements"显示，产生第二次请求的原因是 redirect:，需要用addFlashAttribute在第二次请求临时保存显示
		
		
		redirectAttributes.addAttribute("employeeId",employeeId);
		redirectAttributes.addAttribute("entitlementYear",entitlementYear);
		
		return "redirect:/entitlements";
		//这里redirect，因为可能修改了数据库，保存后重新展示，同时避免反复刷新提交多个（suria Mentioned）
		
	}
	
	@GetMapping("/entitlements/search")
	public String searchEmployees(
			@RequestParam(required = false) Long queryEmployeeId,
			@RequestParam(required = false) String queryEmployeeName,
			HttpSession session,
			Model model) {
		
		Employee currentUser = getCurrentUser(session);
		if (currentUser == null) {
			return "redirect:/employee/login";
		}
		
		List<Employee> canBeSearchedEmployees = getQueryableEmployees(currentUser);
		List<Employee> searchedEmployees = new ArrayList<>();
		
		
		if(queryEmployeeId != null) {
			for(Employee employee : canBeSearchedEmployees) {
				if(employee.getId().equals(queryEmployeeId)) {
					searchedEmployees.add(employee);
				}
			}
		} else if (queryEmployeeName != null && !queryEmployeeName.trim().isEmpty()) {
			String keyword = queryEmployeeName.trim().toLowerCase();
			
			for(Employee employee : canBeSearchedEmployees) {
				if(employee.getName() != null && employee.getName().toLowerCase().contains(keyword)) {
					searchedEmployees.add(employee);
				}
			}
		} 
		
		model.addAttribute("currentUser", currentUser);
		model.addAttribute("toBeViewedEmployees", searchedEmployees);
		model.addAttribute("selectedId", null);
		model.addAttribute("selectedYear", LocalDate.now().getYear());
		model.addAttribute("canEdit", currentUser.getRole() == EmployeeRole.ADMIN);
		
		if(searchedEmployees.isEmpty()) {
			model.addAttribute("message", "No matching employees.");
		}
		
		return "entitlement";
		//这里不redirect，因为只是重新渲染出查到的页面
			
		}

		
		
	}
	
	
			
	
	

	

	


