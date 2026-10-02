package com.group5.cats.repository;
import java.util.List;
import  java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.group5.cats.model.Employee;
import org.springframework.stereotype.Repository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);
    
    List<Employee> findBySupervisor_Id(Long supervisorId);

}
