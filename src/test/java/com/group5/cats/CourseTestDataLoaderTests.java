package com.group5.cats;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.group5.cats.model.Category;
import com.group5.cats.model.TrainingProvider;
import com.group5.cats.repository.*;
import com.group5.cats.service.CourseService;

class CourseTestDataLoaderTests {
    @Test
    void firstEmptyDatabaseSeedsCoursesThroughTheSameServiceAsAdmin() {
        CourseRepository courses = mock(CourseRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        TrainingProviderRepository providers = mock(TrainingProviderRepository.class);
        CourseService service = mock(CourseService.class);
        when(categories.findByNameIgnoreCase(anyString())).thenAnswer(invocation -> {
            Category category = new Category(invocation.getArgument(0), "", true);
            category.setId(1L);
            return Optional.of(category);
        });
        when(providers.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(providers.save(any())).thenAnswer(invocation -> {
            TrainingProvider provider = invocation.getArgument(0);
            provider.setId(2L);
            return provider;
        });
        new CourseTestDataLoader(courses, employees, categories, providers, service).run();
        verify(service, times(18)).createCourse(anyString(), eq(1L), eq(2L), anyDouble(), anyString());
        verify(service, times(6)).createCourse(anyString(), eq(1L), eq(2L), eq(0.0), anyString());
        verify(providers, times(3)).save(any());
        verify(courses, never()).save(any());
    }

    @Test
    void deletingAllCoursesDoesNotReseedWhenEmployeesAlreadyExist() {
        CourseRepository courses = mock(CourseRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        when(employees.count()).thenReturn(5L);
        CategoryRepository categories = mock(CategoryRepository.class);
        TrainingProviderRepository providers = mock(TrainingProviderRepository.class);
        CourseService service = mock(CourseService.class);
        new CourseTestDataLoader(courses, employees, categories, providers, service).run();
        verifyNoInteractions(courses, categories, providers, service);
    }

    @Test
    void existingCoursesAreNotOverwrittenEvenBeforeEmployeesAreSeeded() {
        CourseRepository courses = mock(CourseRepository.class);
        when(courses.count()).thenReturn(1L);
        CategoryRepository categories = mock(CategoryRepository.class);
        TrainingProviderRepository providers = mock(TrainingProviderRepository.class);
        CourseService service = mock(CourseService.class);
        new CourseTestDataLoader(courses, mock(EmployeeRepository.class), categories, providers, service).run();
        verifyNoInteractions(categories, providers, service);
    }
}
