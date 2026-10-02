package com.group5.cats.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
		
		

		
		model.addAttribute("toBeViewedEmployees", toBeViewedEmployees);
		model.addAttribute("employeeId", selectedId);
		model.addAttribute("entitlementYear", selectedYear);
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
			model.addAttribute("message", "No entitlement record for this employeeId and year.");
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
	
	

	

	

}
