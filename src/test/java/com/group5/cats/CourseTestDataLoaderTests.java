package com.group5.cats;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.group5.cats.model.Category;
import com.group5.cats.model.Course;
import com.group5.cats.model.TrainingProvider;
import com.group5.cats.repository.*;
import com.group5.cats.service.CourseService;

class CourseTestDataLoaderTests {
    @Test
    void firstEmptyDatabaseSeedsCoursesThroughTheSameServiceAsAdmin() {
        CourseRepository courses = mock(CourseRepository.class);
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
        new CourseTestDataLoader(courses, categories, providers, service).run();
        verify(service, times(18)).createCourse(anyString(), eq(1L), eq(2L), anyDouble(), anyString());
        verify(service, times(6)).createCourse(anyString(), eq(1L), eq(2L), eq(0.0), anyString());
        verify(providers, times(3)).save(any());
        verify(courses, never()).save(any());
    }

    @Test
    void existingCourseIsSkippedAndOtherSamplesAreAdded() {
        CourseRepository courses = mock(CourseRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        TrainingProviderRepository providers = mock(TrainingProviderRepository.class);
        CourseService service = mock(CourseService.class);
        when(categories.findByNameIgnoreCase(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            Category category = new Category(name, "", false);
            category.setId((long) java.util.List.of("INTERNAL", "EXTERNAL", "CERTIFICATION").indexOf(name) + 1);
            return Optional.of(category);
        });
        when(providers.findByNameIgnoreCase(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            TrainingProvider provider = new TrainingProvider(name);
            provider.setId((long) java.util.List.of("ISS", "AWS", "Demo Skills Academy").indexOf(name) + 1);
            return Optional.of(provider);
        });
        when(courses.findByTitleIgnoreCaseAndCategory_IdAndProvider_Id("Java", 2L, 2L))
                .thenReturn(Optional.of(new Course()));
        new CourseTestDataLoader(courses, categories, providers, service).run();
        verify(service, times(17)).createCourse(anyString(), anyLong(), anyLong(), anyDouble(), anyString());
        verify(service, never()).createCourse(eq("Java"), eq(2L), eq(2L), anyDouble(), anyString());
        verify(providers, never()).save(any());
        verify(courses, never()).save(any());
    }
}
