package com.group5.cats.service;
import java.util.Optional;
import com.group5.cats.model.Employee;

public interface AuthService {
    Optional<Employee> login(String username, String password);

}
