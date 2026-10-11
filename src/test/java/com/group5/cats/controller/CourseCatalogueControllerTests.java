package com.group5.cats.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.group5.cats.model.*;
import com.group5.cats.service.*;

class CourseCatalogueControllerTests {
    @Test
    void onlyAdminCanAddAndRemoveCommonMembership() throws Exception {
        CommonCourseService common = mock(CommonCourseService.class);
        CourseService courses = mock(CourseService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminCommonCourseController(common, courses)).build();
        mvc.perform(post("/admin/common-courses/8/add")).andExpect(redirectedUrl("/admin/login"));
        for (EmployeeRole role : List.of(EmployeeRole.REGULAR_STAFF, EmployeeRole.MANAGER)) {
            Employee user = new Employee(); user.setRole(role);
            mvc.perform(get("/admin/common-courses").sessionAttr("loggedInUser", user)).andExpect(status().isForbidden());
            mvc.perform(post("/admin/common-courses/8/add").sessionAttr("loggedInUser", user)).andExpect(status().isForbidden());
            mvc.perform(post("/admin/common-courses/3/delete").sessionAttr("loggedInUser", user)).andExpect(status().isForbidden());
        }
        verifyNoInteractions(common, courses);
        Employee admin = new Employee(); admin.setRole(EmployeeRole.ADMIN);
        mvc.perform(post("/admin/common-courses/8/add").sessionAttr("loggedInUser", admin)
                .param("title", "Forged").param("fee", "1").param("categoryId", "99"))
                .andExpect(redirectedUrl("/admin/common-courses"));
        verify(common).addCommonCourse(8L);
        mvc.perform(post("/admin/common-courses/3/delete").sessionAttr("loggedInUser", admin))
                .andExpect(redirectedUrl("/admin/common-courses"));
        verify(common).deleteCommonCourse(3L);
        verifyNoInteractions(courses);
    }

    @Test
    void oldFormsRedirectAndOldWritesAreUnavailable() throws Exception {
        CommonCourseService common = mock(CommonCourseService.class);
        CourseService courses = mock(CourseService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminCommonCourseController(common, courses)).build();
        Employee admin = new Employee(); admin.setRole(EmployeeRole.ADMIN);
        for (String path : List.of("/admin/common-courses/new", "/admin/common-courses/1/edit")) {
            mvc.perform(get(path).sessionAttr("loggedInUser", admin)).andExpect(redirectedUrl("/admin/common-courses"));
        }
        for (String path : List.of("/admin/common-courses/save", "/admin/common-courses/1/update")) {
            mvc.perform(post(path).sessionAttr("loggedInUser", admin).param("title", "Forged").param("fee", "1"))
                    .andExpect(status().is4xxClientError());
        }
        verifyNoInteractions(common, courses);
    }

    @Test
    void duplicateMembershipShowsAnErrorWithoutEditingTheCourse() throws Exception {
        CommonCourseService common = mock(CommonCourseService.class);
        CourseService courses = mock(CourseService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminCommonCourseController(common, courses)).build();
        Employee admin = new Employee(); admin.setRole(EmployeeRole.ADMIN);
        when(common.addCommonCourse(8L)).thenReturn("This course is already in the common catalogue.");
        mvc.perform(post("/admin/common-courses/8/add").sessionAttr("loggedInUser", admin))
                .andExpect(redirectedUrl("/admin/common-courses"))
                .andExpect(flash().attribute("errorMessage", "This course is already in the common catalogue."));
        verifyNoInteractions(courses);
    }

    @Test
    void categoryMaintenanceRetainsItsRoleChecks() throws Exception {
        CategoryService categories = mock(CategoryService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminCategoryController(categories)).build();
        Employee user = new Employee(); user.setRole(EmployeeRole.REGULAR_STAFF);
        mvc.perform(post("/admin/categories/save").sessionAttr("loggedInUser", user)).andExpect(status().isForbidden());
        verifyNoInteractions(categories);
        user.setRole(EmployeeRole.ADMIN);
        mvc.perform(post("/admin/categories/save").sessionAttr("loggedInUser", user)
                .param("name", "WORKSHOP").param("description", "Workshop").param("halfDayAllowed", "true"))
                .andExpect(redirectedUrl("/admin/categories"));
        verify(categories).createCategory("WORKSHOP", "Workshop", true);
    }
}
