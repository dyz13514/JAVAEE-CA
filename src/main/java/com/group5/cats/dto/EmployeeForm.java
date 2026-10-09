package com.group5.cats.dto;

import com.group5.cats.model.EmployeeDesignation;
import com.group5.cats.model.EmployeeRole;

public class EmployeeForm {

    private String username;
    private String password;
    private String name;
    private String email;
    private EmployeeRole role;
    private EmployeeDesignation designation;
    private Long supervisorId;

    public EmployeeForm() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public void setRole(EmployeeRole role) {
        this.role = role;
    }

    public EmployeeDesignation getDesignation() {
        return designation;
    }

    public void setDesignation(EmployeeDesignation designation) {
        this.designation = designation;
    }

    public Long getSupervisorId() {
        return supervisorId;
    }

    public void setSupervisorId(Long supervisorId) {
        this.supervisorId = supervisorId;
    }

}