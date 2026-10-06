package com.group5.cats.service;

import java.util.Optional;

import com.group5.cats.model.AnnualEntitlement;

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
	

}
