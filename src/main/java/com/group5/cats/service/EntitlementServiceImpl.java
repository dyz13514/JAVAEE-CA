package com.group5.cats.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.group5.cats.model.AnnualEntitlement;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeDesignation;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.model.EntitlementSummary;
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
		
		if(entitlementYear == null || entitlementYear < 1 || entitlementYear > 9999) {
			throw new IllegalArgumentException("EntitlementYear must be between 1 and 9999.");
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
		
		double daysLimit = getDefaultTrainingDays(employee.getDesignation());
		
		AnnualEntitlement entitlement = new AnnualEntitlement(
				employee, 
				entitlementYear,
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
		
		if(!Double.isFinite(trainingDaysLimit) || trainingDaysLimit < 0) {
			throw new IllegalArgumentException("TrainingDaysLimit must be a finite number greater than or equal to zero.");
		}
		
		if(!Double.isFinite(trainingBudget) || trainingBudget < 0) {
			throw new IllegalArgumentException("TrainingBudget must be a finite number greater than or equal to zero.");
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
	
	@Override
	public List<CourseApplication> findOccupyingApplications(
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
		List<CourseApplication> applications = findOccupyingApplications(employeeId, entitlementYear);
		double occupiedDays= 0.0;
		
		for (CourseApplication application : applications) { 
			occupiedDays += application.getTrainingDays();
		}
		
		return occupiedDays;
	}
	
	@Override
	public double getOccupiedTrainingBudget(Long employeeId, Integer entitlementYear) {
		List<CourseApplication> applications = findOccupyingApplications(employeeId, entitlementYear);
		double occupiedBudget = 0.0;
		
		for (CourseApplication application : applications) { 
			String category = application.getCategory();
			if ("EXTERNAL".equals(category) || "CERTIFICATION".equals(category) ) {
				occupiedBudget += application.getFee();
			}
		}
		
		return occupiedBudget;
	}
	
	@Override
	public Optional<EntitlementSummary> getEntitlementSummary(Long employeeId, Integer entitlementYear) {
		
		Optional<AnnualEntitlement>result = findEntitlement(employeeId, entitlementYear);
		
		if (result.isEmpty()) {
			return Optional.empty();
		}
		
		AnnualEntitlement entitlement = result.get();
		
		List<CourseApplication> applications =  findOccupyingApplications(employeeId, entitlementYear);
		
		double occupiedDays = 0.0;
		double occupiedBudget = 0.0;
		
		for (CourseApplication application : applications) { 
			occupiedDays += application.getTrainingDays();
			String category = application.getCategory();
			if ("EXTERNAL".equals(category) || "CERTIFICATION".equals(category) ) {
				occupiedBudget += application.getFee();
			}
		}
		
		EntitlementSummary summary = new EntitlementSummary();
		
		summary.setEmployeeId(entitlement.getEmployee().getId());
		summary.setEmployeeName(entitlement.getEmployee().getName());
		summary.setEntitlementYear(entitlement.getEntitlementYear());
		
		summary.setOccupiedBudget(occupiedBudget);
		summary.setOccupiedDays(occupiedDays);
		
		summary.setRemainingBudget(entitlement.getTrainingBudget() - occupiedBudget);
		summary.setRemainingDays(entitlement.getTrainingDaysLimit() - occupiedDays);
		
		summary.setTrainingBudget(entitlement.getTrainingBudget());
		summary.setTrainingDaysLimit(entitlement.getTrainingDaysLimit());
		
		
		return Optional.of(summary);
		
	}
	

	@Override
     public List<Employee> getQueryableEmployees(Employee currentUser) {
		
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
	
	@Override
	public void deleteEntitlement(
			Long employeeId, Integer entitlementYear) {
		
		Optional<AnnualEntitlement>result = findEntitlement(employeeId, entitlementYear);

		if(result.isEmpty()) {
			throw new IllegalArgumentException("No entitlement record for this employee in this year.");
		}
		
		annualEntitlementRepository.delete(result.get());
	}
	
	@Override
	public String createPatchDefaultEntitlementsForCurrentYear() {

	    int currentYear = LocalDate.now().getYear();

	    int createdCount = 0;
	    int existingCount = 0;
	    int skippedCount = 0;

	    List<Long> skippedEmployeeIds = new ArrayList<>();

	    List<Employee> employees = employeeRepository.findAll();

	    for (Employee employee : employees) {

	        if (findEntitlement(employee.getId(), currentYear).isPresent()) {

	            existingCount++;

	        } else if (employee.getDesignation() == null) {

	            skippedCount++;
	            skippedEmployeeIds.add(employee.getId());

	        } else {

	            createDefaultEntitlement(employee.getId(), currentYear);
	            createdCount++;
	        }
	    }

	    String message = "Year " + currentYear
	            + ": created " + createdCount
	            + ", already existed " + existingCount
	            + ", skipped (missing designation) " + skippedCount + ".";

	    if (skippedCount > 0) {
	        message += " Skipped employee IDs: " + skippedEmployeeIds;
	    }

	    return message;
	}

}
