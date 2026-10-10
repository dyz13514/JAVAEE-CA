package com.group5.cats;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;

class CourseDataLoaderTests {
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void emptyCatalogueGetsSixCoursesAndSecondRunLeavesThemAlone() throws Exception {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        CommonCourseRepository courses = mock(CommonCourseRepository.class);
        TrainingProviderRepository providers = mock(TrainingProviderRepository.class);
        when(employees.count()).thenReturn(0L, 5L);
        when(courses.count()).thenReturn(0L, 6L);
        when(providers.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(providers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CategoryRepository categories = mock(CategoryRepository.class);
        when(categories.findByNameIgnoreCase(anyString())).thenAnswer(invocation ->
                Optional.of(CategoryFixtures.category(invocation.getArgument(0))));
        when(employees.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        DataLoader loader = new DataLoader(employees, categories, courses, providers);
        loader.run();
        loader.run();
        ArgumentCaptor<Iterable<CommonCourse>> records = ArgumentCaptor.forClass((Class) Iterable.class);
        verify(courses, times(1)).saveAll(records.capture());
        List<CommonCourse> seeded = (List<CommonCourse>) records.getValue();
        assertEquals(6, seeded.size());
        assertEquals(2, seeded.stream().filter(c -> c.getCategory().getName().equals("INTERNAL") && c.getFee() == 0).count());
        assertTrue(seeded.stream().allMatch(c -> c.getProvider() != null));
        verify(providers, times(3)).save(any());
        verify(employees, times(1)).save(any());
    }

    @Test
    void existingCatalogueAndProvidersAreNotOverwritten() throws Exception {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        CommonCourseRepository courses = mock(CommonCourseRepository.class);
        TrainingProviderRepository providers = mock(TrainingProviderRepository.class);
        when(employees.count()).thenReturn(5L);
        when(courses.count()).thenReturn(1L);
        CategoryRepository categories = mock(CategoryRepository.class);
        new DataLoader(employees, categories, courses, providers).run();
        verify(courses, never()).saveAll(any());
        verifyNoInteractions(providers);
    }
}
