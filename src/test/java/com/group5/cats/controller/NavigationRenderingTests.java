package com.group5.cats.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;

class NavigationRenderingTests {
    @ParameterizedTest
    @EnumSource(EmployeeRole.class)
    void roleMenusPreserveSupportedDestinationsWithoutExtraPrimaryEntries(EmployeeRole role) throws Exception {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        var views = new ThymeleafViewResolver(); views.setTemplateEngine(engine); views.setCharacterEncoding("UTF-8");
        var mvc = MockMvcBuilders.standaloneSetup(new Preview()).setViewResolvers(views).build();
        Employee user = new Employee(); user.setId(1L); user.setName("Navigation reviewer"); user.setRole(role);
        var session = new MockHttpSession(); session.setAttribute("loggedInUser", user);
        for (String active : List.of("home", "history", "apply", "claims", "approvals", "team-claims", "calendar", "reports", "employees", "common-courses", "categories", "providers", "entitlement", "training-ledger", "reimbursements", "holidays", "notifications")) {
            String html = mvc.perform(get("/navigation-preview").session(session).param("active", active)
                    .param("scope", active.equals("calendar") ? "team" : "personal"))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            var matcher = Pattern.compile("data-nav-primary=\"([^\"]+)\"").matcher(html);
            var names = new java.util.ArrayList<String>(); while (matcher.find()) names.add(matcher.group(1));
            assertEquals(role == EmployeeRole.ADMIN ? List.of("overview", "employees", "courses", "finance", "records", "settings")
                    : role == EmployeeRole.MANAGER ? List.of("overview", "catalogue", "training", "claims", "approvals", "team", "finance")
                    : List.of("overview", "catalogue", "training", "claims", "finance"), names);
            String sidebar = html.substring(html.indexOf("<aside"), html.indexOf("</aside>"));
            assertFalse(sidebar.contains("/employee/apply"));
            if (role == EmployeeRole.ADMIN) {
                for (String url : List.of("/admin/employees", "/admin/common-courses", "/admin/categories", "/admin/providers", "/entitlements", "/admin/claims", "/training-ledger", "/course-view", "/admin/public-holidays", "/admin/notifications")) assertTrue(sidebar.contains(url), url);
                assertFalse(sidebar.contains("/manager/"));
                if (List.of("categories", "providers", "common-courses").contains(active)) assertTrue(sidebar.contains("class=\"nav-group\" open=\"open\""));
            } else {
                assertFalse(sidebar.contains("/admin/"));
                assertTrue(html.contains("employeeId=17&amp;year=2025"));
                assertTrue(html.contains("employeeId=17&amp;entitlementYear=2025"));
                assertEquals(role == EmployeeRole.MANAGER, sidebar.contains("/manager/approvals"));
            }

        }
    }
    @Controller
    static class Preview {
        @GetMapping("/navigation-preview") String preview(@RequestParam String active, @RequestParam String scope, Model model) {
            model.addAttribute("active", active); model.addAttribute("navScope", scope);
            model.addAttribute("selectedId", 17L); model.addAttribute("selectedYear", 2025);
            return "navigation-preview";
        }
    }
}
