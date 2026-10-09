package com.group5.cats.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;



@Entity
@Table(name = "employees")
public class Employee {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String name;
    @Column(length = 254)
    private String email;
    
    @ManyToOne
    @JoinColumn(name = "supervisor_id")
    private Employee supervisor; 
    
    @Enumerated(EnumType.STRING)
    private EmployeeDesignation designation;
    
    @Enumerated(EnumType.STRING)
    private EmployeeRole role;

    
    public Employee() {
    }

    
    public Employee(String username, String password, String name, EmployeeRole role) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.role = role;
    }
    

    public Employee(String username, String password, String name, Employee supervisor,
			EmployeeDesignation designation, EmployeeRole role) {
		this.username = username;
		this.password = password;
		this.name = name;
		this.supervisor = supervisor;
		this.designation = designation;
		this.role = role;
	}


	public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
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


	public Employee getSupervisor() {
		return supervisor;
	}


	public void setSupervisor(Employee supervisor) {
		this.supervisor = supervisor;
	}


	public EmployeeDesignation getDesignation() {
		return designation;
	}


	public void setDesignation(EmployeeDesignation designation) {
		this.designation = designation;
	}
    

    
}
