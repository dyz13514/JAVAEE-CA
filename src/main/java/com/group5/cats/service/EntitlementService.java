package com.group5.cats.service;

import java.util.List;
import java.util.Optional;

import com.group5.cats.model.AnnualEntitlement;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EntitlementSummary;

public interface EntitlementService{
	
	Optional<AnnualEntitlement> findEntitlement(
			Long employeeId, Integer entitlementYear);
	
	AnnualEntitlement createDefaultEntitlement(
			Long employeeId, Integer entitlementYear);
	
	AnnualEntitlement setEntitlement(
			Long employeeId,
			Integer entitlementYear,
			double trainingDaysLimit,
			double trainingBudget
			);
	
	double getOccupiedTrainingDays(
			Long employeeId, Integer entitlementYear);
	
	double getOccupiedTrainingBudget(
			Long employeeId, Integer entitlementYear);
	
	Optional<EntitlementSummary> getEntitlementSummary(
			Long employeeId, Integer entitlementYear);
	
	List<Employee> getQueryableEmployees(Employee currentUser);
	
	void deleteEntitlement(
			Long employeeId, Integer entitlementYear);
	

}
