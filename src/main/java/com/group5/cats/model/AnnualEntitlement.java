package com.group5.cats.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "annual_entitlements")
public class AnnualEntitlement {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "employee_id")
	private Employee employee;
	
	private Integer entitlementYear;
	
	private double trainingDaysLimit;
	
	private double trainingBudget;

	
	
	public AnnualEntitlement() {
	}

	public AnnualEntitlement(Employee employee, Integer entitlementYear, double trainingDaysLimit,
			double trainingBudget) {
		this.employee = employee;
		this.entitlementYear = entitlementYear;
		this.trainingDaysLimit = trainingDaysLimit;
		this.trainingBudget = trainingBudget;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	public Integer getEntitlementYear() {
		return entitlementYear;
	}

	public void setEntitlementYear(Integer entitlementYear) {
		this.entitlementYear = entitlementYear;
	}

	public double getTrainingDaysLimit() {
		return trainingDaysLimit;
	}

	public void setTrainingDaysLimit(double trainingDaysLimit) {
		this.trainingDaysLimit = trainingDaysLimit;
	}

	public double getTrainingBudget() {
		return trainingBudget;
	}

	public void setTrainingBudget(double trainingBudget) {
		this.trainingBudget = trainingBudget;
	}
	
	
	

	
}
