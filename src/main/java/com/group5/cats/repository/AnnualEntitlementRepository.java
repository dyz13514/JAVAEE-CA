package com.group5.cats.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group5.cats.model.AnnualEntitlement;

public interface AnnualEntitlementRepository extends JpaRepository<AnnualEntitlement, Long> {
	Optional<AnnualEntitlement> findByEmployeeIdAndEntitlementYear(
			Long employeeId, Integer entitlementYear);

}
