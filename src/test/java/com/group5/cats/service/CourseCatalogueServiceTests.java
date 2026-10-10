package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.group5.cats.CategoryFixtures;
import com.group5.cats.dto.CommonCourseForm;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;

class CourseCatalogueServiceTests {
    private CommonCourseRepository courses;
    private CategoryRepository categories;
    private TrainingProviderRepository providers;
    private CourseApplicationRepository applications;
    private CommonCourseServiceImpl service;
    private CategoryServiceImpl categoryService;
    private Category external;
    private TrainingProvider provider;

    @BeforeEach
    void setUp() {
        courses = mock(CommonCourseRepository.class);
        categories = mock(CategoryRepository.class);
        providers = mock(TrainingProviderRepository.class);
        applications = mock(CourseApplicationRepository.class);
        service = new CommonCourseServiceImpl(courses, categories, providers);
        categoryService = new CategoryServiceImpl(categories, courses, applications);
        external = CategoryFixtures.category("EXTERNAL");
        provider = new TrainingProvider("NUS"); provider.setId(3L);
        when(categories.findById(2L)).thenReturn(Optional.of(external));
        when(providers.findById(3L)).thenReturn(Optional.of(provider));
    }

    private CommonCourseForm form() {
        CommonCourseForm form = new CommonCourseForm();
        form.setTitle("Java"); form.setCategoryId(2L); form.setProviderId(3L);
        form.setFee(200.0); form.setIntroduction("Learn Java.");
        return form;
    }

    @Test
    void catalogueValidatesReferencesAndDuplicateIdentity() {
        assertNull(service.createCommonCourse(form()));
        verify(courses).save(argThat(course -> course.getCategory() == external && course.getProvider() == provider));
        when(courses.existsByTitleIgnoreCaseAndCategory_IdAndProvider_Id("Java", 2L, 3L)).thenReturn(true);
        assertNotNull(service.createCommonCourse(form()));
        CommonCourseForm invalid = form(); invalid.setCategoryId(999L);
        assertNotNull(service.createCommonCourse(invalid));
        invalid = form(); invalid.setProviderId(999L);
        assertNotNull(service.createCommonCourse(invalid));
    }

    @Test
    void internalReferenceFeeMayBeRecordedAndInvalidFeeIsRejected() {
        when(categories.findById(2L)).thenReturn(Optional.of(CategoryFixtures.category("INTERNAL")));
        assertNull(service.createCommonCourse(form()));
        for (double fee : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            CommonCourseForm invalid = form(); invalid.setFee(fee);
            assertNotNull(service.createCommonCourse(invalid));
        }
    }

    @Test
    void removingCatalogueDoesNotDeleteApplications() {
        CommonCourse course = new CommonCourse("Java", external, provider, 100); course.setId(5L);
        when(courses.findById(5L)).thenReturn(Optional.of(course));
        assertNull(service.deleteCommonCourse(5L));
        verify(courses).delete(course);
        verifyNoInteractions(applications);
    }

    @Test
    void originalCategoryNameAndHalfDayAreProtectedButDescriptionCanChange() {
        Category internal = CategoryFixtures.category("INTERNAL");
        when(categories.findById(1L)).thenReturn(Optional.of(internal));
        when(categories.findByNameIgnoreCase("INTERNAL")).thenReturn(Optional.of(internal));
        assertNotNull(categoryService.updateCategory(1L, "Renamed", "New", true));
        assertNotNull(categoryService.updateCategory(1L, "INTERNAL", "New", false));
        assertNull(categoryService.updateCategory(1L, "INTERNAL", "New", true));
        assertEquals("New", internal.getDescription());
        assertNotNull(categoryService.deleteCategory(1L));
        verify(categories, never()).delete(any());
    }

    @Test
    void categoryInUseCannotBeDeletedAndUnusedCategoryCan() {
        Category category = CategoryFixtures.category("WORKSHOP");
        when(categories.findById(4L)).thenReturn(Optional.of(category));
        when(applications.existsByCategory_Id(4L)).thenReturn(true);
        assertNotNull(categoryService.deleteCategory(4L));
        when(applications.existsByCategory_Id(4L)).thenReturn(false);
        when(courses.existsByCategory_Id(4L)).thenReturn(true);
        assertNotNull(categoryService.deleteCategory(4L));
        when(courses.existsByCategory_Id(4L)).thenReturn(false);
        assertNull(categoryService.deleteCategory(4L));
        verify(categories).delete(category);
    }

    @Test
    void filtersUseCategoryAndProviderWithoutSchedule() {
        CommonCourse course = new CommonCourse("Java", external, provider, 100);
        course.setIntroduction("Learn programming");
        when(courses.findAllByOrderByTitleAsc()).thenReturn(List.of(course));
        assertEquals(List.of(course), service.findCommonCourses("java", 2L, 3L));
        assertTrue(service.findCommonCourses("SQL", null, null).isEmpty());
        assertTrue(service.findCommonCourses("", 1L, null).isEmpty());
    }
}
