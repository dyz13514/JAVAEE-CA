package com.group5.cats.controller;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.group5.cats.model.*;
import com.group5.cats.service.*;

class CourseCatalogueControllerTests {
    @Test
    void adminCanMaintainBothCatalogueAndCategoriesAndStaffIsBlocked() throws Exception {
        CommonCourseService courses = mock(CommonCourseService.class);
        CategoryService categories = mock(CategoryService.class);
        var mvc = MockMvcBuilders.standaloneSetup(
                new AdminCommonCourseController(courses, categories, mock(TrainingProviderService.class)),
                new AdminCategoryController(categories)).build();
        Employee user = new Employee();
        user.setRole(EmployeeRole.REGULAR_STAFF);
        mvc.perform(get("/admin/categories").sessionAttr("loggedInUser", user)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/categories/save").sessionAttr("loggedInUser", user)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/categories/1/delete").sessionAttr("loggedInUser", user)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/common-courses/save").sessionAttr("loggedInUser", user)).andExpect(redirectedUrl("/employee/home"));
        verifyNoInteractions(categories, courses);
        user.setRole(EmployeeRole.ADMIN);
        mvc.perform(post("/admin/categories/save").sessionAttr("loggedInUser", user)
                .param("name", "WORKSHOP").param("description", "Workshop").param("halfDayAllowed", "true"))
                .andExpect(redirectedUrl("/admin/categories"));
        verify(categories).createCategory("WORKSHOP", "Workshop", true);
        mvc.perform(post("/admin/common-courses/save").sessionAttr("loggedInUser", user)
                .param("title", "Java").param("categoryId", "2").param("providerId", "3")
                .param("fee", "100").param("introduction", "Learn Java"))
                .andExpect(redirectedUrl("/admin/common-courses"));
        verify(courses).createCommonCourse(any());
    }

    @Test
    void malformedFeesAndMissingLoginDoNotCallServices() throws Exception {
        CommonCourseService courses = mock(CommonCourseService.class);
        CategoryService categories = mock(CategoryService.class);
        var mvc = MockMvcBuilders.standaloneSetup(
                new AdminCommonCourseController(courses, categories, mock(TrainingProviderService.class)),
                new AdminCategoryController(categories)).build();
        mvc.perform(post("/admin/categories/save")).andExpect(redirectedUrl("/admin/login"));
        Employee admin = new Employee(); admin.setRole(EmployeeRole.ADMIN);
        mvc.perform(post("/admin/common-courses/save").sessionAttr("loggedInUser", admin).param("fee", "invalid"))
                .andExpect(view().name("admin/common-course-form")).andExpect(model().attributeExists("errorMessage"));
        verifyNoInteractions(courses);
    }
}
