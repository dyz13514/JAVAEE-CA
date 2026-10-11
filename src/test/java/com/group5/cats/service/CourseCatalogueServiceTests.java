package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.group5.cats.CategoryFixtures;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;

class CourseCatalogueServiceTests {
    private CommonCourseRepository courses;
    private CourseRepository records;
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
        records = mock(CourseRepository.class);
        categories = mock(CategoryRepository.class);
        providers = mock(TrainingProviderRepository.class);
        applications = mock(CourseApplicationRepository.class);
        service = new CommonCourseServiceImpl(courses, records);
        categoryService = new CategoryServiceImpl(categories, records, applications);
        external = CategoryFixtures.category("EXTERNAL");
        provider = new TrainingProvider("NUS"); provider.setId(3L);
        when(categories.findById(2L)).thenReturn(Optional.of(external));
        when(providers.findById(3L)).thenReturn(Optional.of(provider));
    }

    @Test
    void membershipReferencesAnExistingCourseAndRejectsDuplicates() {
        Course course = new Course("Java", external, provider, 200); course.setId(8L);
        when(records.findById(8L)).thenReturn(Optional.of(course));
        assertNull(service.addCommonCourse(8L));
        verify(courses).save(argThat(entry -> entry.getCourse() == course));
        when(courses.existsByCourse_Id(8L)).thenReturn(true);
        assertNotNull(service.addCommonCourse(8L));
        verify(records, never()).save(any());
    }

    @Test
    void unavailableCoursesCannotBeAddedToCommon() {
        Course course = new Course("Java", external, provider, 200); course.setActive(false);
        when(records.findById(8L)).thenReturn(Optional.of(course));
        assertNotNull(service.addCommonCourse(8L));
        assertNotNull(service.addCommonCourse(999L));
        assertNotNull(service.addCommonCourse(null));
        assertNotNull(service.addCommonCourse(0L));
        verify(courses, never()).save(any());
    }

    @Test
    void removingCatalogueDoesNotChangeCourseOrApplications() {
        CommonCourse entry = new CommonCourse("Java", external, provider, 100); entry.setId(5L);
        when(courses.findById(5L)).thenReturn(Optional.of(entry));
        assertNull(service.deleteCommonCourse(5L));
        verify(courses).delete(entry);
        assertTrue(entry.getCourse().isActive());
        assertEquals(100, entry.getFee());
        verifyNoInteractions(records, applications);
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
        when(records.existsByCategory_Id(4L)).thenReturn(true);
        assertNotNull(categoryService.deleteCategory(4L));
        when(records.existsByCategory_Id(4L)).thenReturn(false);
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
