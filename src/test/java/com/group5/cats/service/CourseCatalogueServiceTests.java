package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.group5.cats.dto.*;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;

class CourseCatalogueServiceTests {
    private CourseRepository courses;
    private CommonCourseRepository common;
    private CourseApplicationRepository applications;
    private TrainingProviderRepository providers;
    private CourseServiceImpl courseService;
    private CommonCourseServiceImpl commonService;
    private Course course;

    @BeforeEach
    void setUp() {
        courses = mock(CourseRepository.class);
        common = mock(CommonCourseRepository.class);
        applications = mock(CourseApplicationRepository.class);
        providers = mock(TrainingProviderRepository.class);
        TrainingProvider provider = new TrainingProvider("NUS-ISS");
        provider.setId(1L);
        course = new Course("Java", "EXTERNAL", provider, 100);
        course.setId(2L);
        when(courses.findById(2L)).thenReturn(Optional.of(course));
        when(providers.findById(1L)).thenReturn(Optional.of(provider));
        courseService = new CourseServiceImpl(courses, providers, common, applications);
        commonService = new CommonCourseServiceImpl(common, courses);
    }

    @Test
    void addingToCommonCatalogueStoresOnlyTheCourseReference() {
        CommonCourseForm form = selection(2L);
        assertNull(commonService.createCommonCourse(form));
        verify(common).save(argThat(entry -> entry.getCourse() == course));
        verify(courses, never()).save(any());
    }

    @Test
    void duplicateAndMissingSelectionsAreRejected() {
        when(common.existsByCourse_Id(2L)).thenReturn(true);
        assertNotNull(commonService.createCommonCourse(selection(2L)));
        assertNotNull(commonService.createCommonCourse(selection(999L)));
        assertNotNull(commonService.createCommonCourse(selection(null)));
        verify(common, never()).save(any());
    }

    @Test
    void replacingSelectionRejectsACourseAlreadyInAnotherEntry() {
        CommonCourse entry = new CommonCourse(course);
        when(common.findById(5L)).thenReturn(Optional.of(entry));
        when(common.existsByCourse_IdAndIdNot(2L, 5L)).thenReturn(true);
        assertNotNull(commonService.updateCommonCourse(5L, selection(2L)));
        verify(common, never()).save(any());
    }

    @Test
    void removingCommonEntryDoesNotDeleteTheTotalCourse() {
        CommonCourse entry = new CommonCourse(course);
        when(common.findById(5L)).thenReturn(Optional.of(entry));
        assertNull(commonService.deleteCommonCourse(5L));
        verify(common).delete(entry);
        verify(courses, never()).delete(any());
    }

    @Test
    void referencedCourseCannotBeDeleted() {
        when(common.existsByCourse_Id(2L)).thenReturn(true);
        assertNotNull(courseService.deleteCourse(2L));
        when(common.existsByCourse_Id(2L)).thenReturn(false);
        when(applications.existsByCourse_Id(2L)).thenReturn(true);
        assertNotNull(courseService.deleteCourse(2L));
        verify(courses, never()).delete(any());
    }

    @Test
    void unreferencedCourseCanBeDeleted() {
        assertNull(courseService.deleteCourse(2L));
        verify(courses).delete(course);
    }

    @Test
    void providerUsedByANonCommonCourseCannotBeDeleted() {
        when(courses.existsByProvider_Id(1L)).thenReturn(true);
        TrainingProviderServiceImpl service = new TrainingProviderServiceImpl(providers, courses);
        assertNotNull(service.deleteProvider(1L));
        verify(providers, never()).delete(any());
    }

    @Test
    void validCourseIsSavedAndInvalidFeesAreRejected() {
        CourseForm form = new CourseForm();
        form.setIntroduction("An introduction to Java.");
        form.setTitle(" Java ");
        form.setCategory("EXTERNAL");
        form.setProviderId(1L);
        form.setFee(100.0);
        assertNull(courseService.createCourse(form));
        verify(courses).save(argThat(saved -> saved.getTitle().equals("Java")));
        form.setFee(Double.NaN);
        assertNotNull(courseService.createCourse(form));
        form.setFee(-1.0);
        assertNotNull(courseService.createCourse(form));
        form.setFee(100.0);
        form.setCategory("INTERNAL");
        assertNotNull(courseService.createCourse(form));
    }

    @Test
    void filtersCombineKeywordProviderCategoryAndCommonMembership() {
        TrainingProvider otherProvider = new TrainingProvider("Other Academy");
        otherProvider.setId(3L);
        Course second = new Course("Java", "CERTIFICATION", otherProvider, 200);
        second.setId(4L);
        Course third = new Course("Cloud", "EXTERNAL", course.getProvider(), 300);
        third.setId(5L);
        when(courses.findAllByOrderByTitleAsc()).thenReturn(java.util.List.of(course, second, third));
        when(common.findAll()).thenReturn(java.util.List.of(new CommonCourse(course)));

        assertEquals(java.util.List.of(course, second), courseService.findCourses(" JAVA ", "", null, false));
        assertEquals(java.util.List.of(course, third), courseService.findCourses("nus-iss", "", null, false));
        assertEquals(java.util.List.of(course), courseService.findCourses("java", "EXTERNAL", 1L, true));
        assertTrue(courseService.findCourses("java", "EXTERNAL", 3L, false).isEmpty());
        assertTrue(courseService.findCourses("", "CERTIFICATION", null, true).isEmpty());
        assertEquals(3, courseService.findCourses("", "", null, false).size());
    }

    private CommonCourseForm selection(Long id) {
        CommonCourseForm form = new CommonCourseForm();
        form.setCourseId(id);
        return form;
    }
}
