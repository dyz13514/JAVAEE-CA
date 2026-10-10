package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.dto.ApplicationSearch;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:application-pagination;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@Transactional
class ApplicationPaginationTests {
    @Autowired CourseApplicationService service;
    @Autowired CourseApplicationRepository applications;
    @Autowired EmployeeRepository employees;

    @Test
    void pagesContainEveryMatchingRecordExactlyOnceAndRespectYearAndOwner() {
        Employee owner = employee("Page owner", EmployeeRole.REGULAR_STAFF, null);
        Employee outsider = employee("Outsider", EmployeeRole.REGULAR_STAFF, null);
        for (int i = 0; i < 35; i++) application(owner, "Java " + i, LocalDate.now());
        application(owner, "Last year", LocalDate.now().minusYears(1));
        application(outsider, "Java outsider", LocalDate.now());
        var ids = new HashSet<Long>();
        ApplicationSearch search = new ApplicationSearch();
        for (int page = 1; page <= 4; page++) {
            search.setPage(page);
            var result = service.searchEmployeeApplications(owner, true, search);
            assertEquals(35, result.getTotalElements());
            assertEquals(page == 4 ? 5 : 10, result.getNumberOfElements());
            for (CourseApplication row : result) assertTrue(ids.add(row.getId()));
        }
        assertEquals(35, ids.size());
        search.setPage(1);
        assertEquals(36, service.searchEmployeeApplications(owner, false, search).getTotalElements());
        search.setKeyword(" java 0 ");
        assertEquals(1, service.searchEmployeeApplications(owner, true, search).getTotalElements());
    }

    @Test
    void sizeChoicesOutOfRangePagesAndEmptySearchResultsAreHandled() {
        Employee owner = employee("Sizes", EmployeeRole.REGULAR_STAFF, null);
        for (int i = 0; i < 35; i++) application(owner, "Course " + i, LocalDate.now());
        ApplicationSearch search = new ApplicationSearch();
        for (int size : new int[] {10, 20, 25}) {
            search.setSize(size);
            search.setPage(1);
            assertEquals(size, service.searchEmployeeApplications(owner, true, search).getNumberOfElements());
        }
        search.setSize(999);
        search.setPage(Integer.MAX_VALUE);
        var last = service.searchEmployeeApplications(owner, true, search);
        assertEquals(4, search.getPage());
        assertEquals(5, last.getNumberOfElements());
        search.setKeyword("missing");
        assertEquals(0, service.searchEmployeeApplications(owner, true, search).getTotalElements());
        assertEquals(1, search.getPage());
        search.setPage(-2);
        assertEquals(1, search.getPage());
    }

    @Test
    void teamSearchOnlyFindsDirectReportsAndTreatsWildcardsLiterally() {
        Employee manager = employee("Page manager", EmployeeRole.MANAGER, null);
        Employee staff = employee("Alice page", EmployeeRole.REGULAR_STAFF, manager);
        Employee other = employee("Other team", EmployeeRole.REGULAR_STAFF, null);
        application(staff, "100% Java_course", LocalDate.now());
        application(staff, "Java", LocalDate.now());
        application(manager, "100% Java_course", LocalDate.now());
        application(other, "100% Java_course", LocalDate.now());
        ApplicationSearch search = new ApplicationSearch();
        search.setKeyword("ALICE");
        assertEquals(2, service.searchTeamApplications(manager, search).getTotalElements());
        search.setKeyword("%");
        assertEquals(1, service.searchTeamApplications(manager, search).getTotalElements());
        search.setKeyword("_");
        assertEquals(1, service.searchTeamApplications(manager, search).getTotalElements());
        assertThrows(SecurityException.class, () -> service.searchTeamApplications(staff, search));
    }

    private Employee employee(String name, EmployeeRole role, Employee supervisor) {
        return employees.save(new Employee(name, "test-password", name, supervisor,
                EmployeeDesignation.PROFESSIONAL, role));
    }

    private void application(Employee employee, String title, LocalDate date) {
        CourseApplication application = new CourseApplication();
        application.setEmployee(employee);
        application.setCourseTitle(title);
        application.setCategory(com.group5.cats.CategoryFixtures.category("EXTERNAL"));
        application.setProvider("Test provider");
        application.setFromDate(date);
        application.setToDate(date.plusDays(1));
        applications.saveAndFlush(application);
    }
}
