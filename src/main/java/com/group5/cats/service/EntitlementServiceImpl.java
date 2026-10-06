package com.group5.cats.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.group5.cats.model.AnnualEntitlement;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeDesignation;
import com.group5.cats.repository.AnnualEntitlementRepository;
import com.group5.cats.repository.CourseApplicationRepository;
import com.group5.cats.repository.EmployeeRepository;

@Service
public class EntitlementServiceImpl implements EntitlementService {
	
	private final AnnualEntitlementRepository annualEntitlementRepository;
	private final EmployeeRepository employeeRepository;
	private final CourseApplicationRepository courseApplicationRepository;
	
	private static final double ADMINISTRATIVE_DAYS = 50.0;
	private static final double PROFESSIONAL_DAYS = 100.0;
	private static final double DEFAULT_TRAINING_BUDGET = 20000.0;
	
	public EntitlementServiceImpl(AnnualEntitlementRepository annualEntitlementRepository,
			EmployeeRepository employeeRepository, CourseApplicationRepository courseApplicationRepository) {
		this.annualEntitlementRepository = annualEntitlementRepository;
		this.employeeRepository = employeeRepository;
		this.courseApplicationRepository = courseApplicationRepository;
	}
	
	private void validateInputs(
			Long employeeId, Integer entitlementYear) {
		
		if(employeeId == null || employeeId <= 0) {
			throw new IllegalArgumentException("EmployeeId must be positive.");
		}
		
		if(entitlementYear == null || entitlementYear <= 0) {
			throw new IllegalArgumentException("EntitlementYear must be positive.");
		}
	}

	@Override
	public Optional<AnnualEntitlement> findEntitlement(Long employeeId, Integer entitlementYear) {
	
		validateInputs(employeeId, entitlementYear);
	
		return annualEntitlementRepository
				.findByEmployeeIdAndEntitlementYear(employeeId, entitlementYear);
	}

	@Override
	public AnnualEntitlement createDefaultEntitlement(Long employeeId, Integer entitlementYear) {
		
		validateInputs(employeeId, entitlementYear);
		
		Optional<AnnualEntitlement> existing = annualEntitlementRepository
				.findByEmployeeIdAndEntitlementYear(employeeId, entitlementYear);
		
		if(existing.isPresent()) {
			return existing.get();
		}
		
		Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new IllegalArgumentException("EmployeeId " + employeeId + " does not exist."));
		//这里参考了 demo 项目 EmployeeService 中，查询员工不存在时使用lambda orElseThrow 的写法。
		
		double daysLimit = getDefaultTrainingDays(employee.getDesignation());
		
		AnnualEntitlement entitlement = new AnnualEntitlement(
				employee, 
				entitlementYear,
				//这里没有检查entitlementYear是过去还是未来，传入什么entitlementYear录入什么，因为创建的entitlement记录可以录入之前的也可以录入之后的
				daysLimit,
				DEFAULT_TRAINING_BUDGET);
				
		return annualEntitlementRepository.save(entitlement);
	}
	
	private double getDefaultTrainingDays(
			EmployeeDesignation designation) {
		
		if(designation == null ) {
		throw new IllegalArgumentException("Please set the employee's designation first.");
	}
		
		switch (designation) {
		case ADMINISTRATIVE:
			return ADMINISTRATIVE_DAYS;
		case PROFESSIONAL:
			return  PROFESSIONAL_DAYS;
		}
		
		throw new IllegalArgumentException("The designation is not valid.");
		
	}
	
	@Override
	public AnnualEntitlement setEntitlement(
			Long employeeId,
			Integer entitlementYear,
			double trainingDaysLimit,
			double trainingBudget
			) {
		validateInputs(employeeId, entitlementYear);
		
		if(trainingDaysLimit < 0) {
			throw new IllegalArgumentException("TrainingDaysLimit must >= 0.");
		}
		
		if(trainingBudget < 0) {
			throw new IllegalArgumentException("trainingBudget must >= 0.");
		}
		
		Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new IllegalArgumentException("EmployeeId " + employeeId + " does not exist.")); 
		
		Optional<AnnualEntitlement> existing = annualEntitlementRepository
				.findByEmployeeIdAndEntitlementYear(employeeId, entitlementYear);
		
		AnnualEntitlement entitlement;
		
		if(existing.isPresent()) {
			entitlement =  existing.get();
		} else {
			entitlement = new AnnualEntitlement();
			entitlement.setEmployee(employee);
			entitlement.setEntitlementYear(entitlementYear);
		}
		
		entitlement.setTrainingDaysLimit(trainingDaysLimit);
		entitlement.setTrainingBudget(trainingBudget);
		
		return annualEntitlementRepository.save(entitlement);
		
		
		
			}
	
	private List<CourseApplication> getOccupyingApplications(
			Long employeeId, Integer entitlementYear) {
		validateInputs(employeeId, entitlementYear);
		
		Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new IllegalArgumentException("EmployeeId " + employeeId + " does not exist."));
		
		List<CourseApplication> applications = courseApplicationRepository.findByEmployee(employee);
		
		List<CourseApplication> occupyingApplications = new ArrayList<>();
		
		for (CourseApplication application : applications) {
			if (application.getFromDate() != null  && application.getFromDate().getYear() == entitlementYear) {
				String status = application.getStatus();
				if("APPLIED".equals(status) || "UPDATED".equals(status) || "APPROVED".equals(status) || "COMPLETED".equals(status) ) {
					occupyingApplications.add(application);
				}
			}
		}
		
		return occupyingApplications;
	}
	
	@Override
	public double getOccupiedTrainingDays(Long employeeId, Integer entitlementYear) {
		List<CourseApplication> applications = getOccupyingApplications(employeeId, entitlementYear);
		double occupiedDays= 0.0;
		
		for (CourseApplication application : applications) { 
			occupiedDays += application.getTrainingDays();
		}
		
		return occupiedDays;
	}
	
	@Override
	public double getOccupiedTrainingBudget(Long employeeId, Integer entitlementYear) {
		List<CourseApplication> applications = getOccupyingApplications(employeeId, entitlementYear);
		double occupiedBudget = 0.0;
		
		for (CourseApplication application : applications) { 
			String category = application.getCategory();
			if ("EXTERNAL".equals(category) || "CERTIFICATION".equals(category) ) {
				occupiedBudget += application.getFee();
			}
		}
		
		return occupiedBudget;
	}
	
	

}
