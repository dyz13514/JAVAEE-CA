package com.group5.cats.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.AuthService;

class LoginControllerTests {
    private AuthService authService;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        authService = mock(AuthService.class);
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        views.setCharacterEncoding("UTF-8");
        mvc = MockMvcBuilders.standaloneSetup(new LoginController(authService))
                .setViewResolvers(views).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/login", "/employee/login", "/admin/login"})
    void everyEntryShowsTheSameFormWithTheCorrectRole(String path) throws Exception {
        boolean admin = path.startsWith("/admin");
        String endpoint = admin ? "/admin/login" : "/employee/login";
        String html = mvc.perform(get(path).flashAttr("errorMessage", "Invalid credentials"))
                .andExpect(status().isOk()).andExpect(view().name("login"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("data-login-role=\"" + (admin ? "admin" : "employee") + "\""));
        assertTrue(html.contains("action=\"" + endpoint + "\""));
        assertTrue(html.contains("name=\"username\""));
        assertTrue(html.contains("name=\"password\""));
        assertTrue(html.contains("Invalid credentials"));
        assertFalse(html.contains("Choose your login"));
        verifyNoInteractions(authService);
    }

    @ParameterizedTest
    @CsvSource({"REGULAR_STAFF,/employee/login,/employee/home", "MANAGER,/employee/login,/employee/home",
            "ADMIN,/admin/login,/admin/home"})
    void originalEndpointsStillAuthenticateAndCreateSessions(EmployeeRole role, String endpoint, String home) throws Exception {
        Employee user = new Employee();
        user.setRole(role);
        when(authService.login("test-user", "test-password")).thenReturn(Optional.of(user));
        mvc.perform(post(endpoint).param("username", "test-user").param("password", "test-password"))
                .andExpect(redirectedUrl(home)).andExpect(request().sessionAttribute("loggedInUser", user));
        verify(authService).login("test-user", "test-password");
    }

    @ParameterizedTest
    @CsvSource({"REGULAR_STAFF,/admin/login", "MANAGER,/admin/login", "ADMIN,/employee/login"})
    void roleRestrictionsRemainInPlace(EmployeeRole role, String endpoint) throws Exception {
        Employee user = new Employee();
        user.setRole(role);
        when(authService.login("test-user", "test-password")).thenReturn(Optional.of(user));
        mvc.perform(post(endpoint).param("username", "test-user").param("password", "test-password"))
                .andExpect(redirectedUrl(endpoint)).andExpect(flash().attributeExists("errorMessage"))
                .andExpect(request().sessionAttributeDoesNotExist("loggedInUser"));
    }
}
