package com.group5.cats.model;

public class EntitlementSummary {
	
	private Long employeeId;
	private String employeeName;
	private Integer entitlementYear;
	
    private double trainingDaysLimit;
    private double occupiedDays;
    private double remainingDays;
	
	private double trainingBudget;
    private double occupiedBudget;
    private double remainingBudget;
    
    //把页面需要的原记录的信息和本次计算的结果全部放在这一个对象里，也就是DTO-数据传输对象
    
	public EntitlementSummary() {
	}

	public Long getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(Long employeeId) {
		this.employeeId = employeeId;
	}

	public String getEmployeeName() {
		return employeeName;
	}

	public void setEmployeeName(String employeeName) {
		this.employeeName = employeeName;
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

	public double getOccupiedDays() {
		return occupiedDays;
	}

	public void setOccupiedDays(double occupiedDays) {
		this.occupiedDays = occupiedDays;
	}

	public double getRemainingDays() {
		return remainingDays;
	}

	public void setRemainingDays(double remainingDays) {
		this.remainingDays = remainingDays;
	}

	public double getTrainingBudget() {
		return trainingBudget;
	}

	public void setTrainingBudget(double trainingBudget) {
		this.trainingBudget = trainingBudget;
	}

	public double getOccupiedBudget() {
		return occupiedBudget;
	}

	public void setOccupiedBudget(double occupiedBudget) {
		this.occupiedBudget = occupiedBudget;
	}

	public double getRemainingBudget() {
		return remainingBudget;
	}

	public void setRemainingBudget(double remainingBudget) {
		this.remainingBudget = remainingBudget;
	}
	
   //对应关系:Java：getEmployeeId() -> JSON：employeeId -> JavaScript：data.employeeId
	
    
    

}
