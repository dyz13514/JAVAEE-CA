package com.group5.cats;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;

class CourseDataLoaderTests {
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void seedsEighteenVariantsWithFixedFeesAndLeavesThemAloneOnRestart() {
        CourseRepository records = mock(CourseRepository.class);
        CommonCourseRepository common = mock(CommonCourseRepository.class);
        TrainingProviderRepository providers = mock(TrainingProviderRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        when(records.count()).thenReturn(0L, 18L);
        when(providers.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(providers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(categories.findByNameIgnoreCase(anyString())).thenAnswer(invocation ->
                Optional.of(CategoryFixtures.category(invocation.getArgument(0))));
        CourseDataLoader loader = new CourseDataLoader(records, categories, providers);
        loader.run(); loader.run();
        ArgumentCaptor<Iterable<Course>> captured = ArgumentCaptor.forClass((Class) Iterable.class);
        verify(records).saveAll(captured.capture());
        List<Course> seeded = (List<Course>) captured.getValue();
        assertEquals(18, seeded.size());
        verifyNoInteractions(common);
        assertEquals(6, seeded.stream().map(Course::getTitle).distinct().count());
        assertTrue(seeded.stream().allMatch(c -> c.getCategory().getName().equals("INTERNAL")
                ? c.getFee() == 0 : Set.of(200.0, 300.0, 500.0).contains(c.getFee())));
        assertEquals(3, seeded.stream().filter(c -> c.getTitle().equals("Java")).count());
        assertEquals("AWS", seeded.stream().filter(c -> c.getTitle().equals("Java")
                && c.getCategory().getName().equals("EXTERNAL")).findFirst().orElseThrow().getProvider().getName());
    }

    @Test
    void existingCoursesAndProvidersAreNotOverwritten() {
        CourseRepository records = mock(CourseRepository.class);
        when(records.count()).thenReturn(1L);
        CommonCourseRepository common = mock(CommonCourseRepository.class);
        TrainingProviderRepository providers = mock(TrainingProviderRepository.class);
        new CourseDataLoader(records, mock(CategoryRepository.class), providers).run();
        verifyNoInteractions(common, providers);
    }
}
