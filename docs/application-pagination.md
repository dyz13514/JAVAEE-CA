# Course application pagination

## Pages

- `/employee/history`: the signed-in employee or manager's applications for the current year.
- `/manager/approvals`: applications belonging to the manager's direct reports, grouped by employee name through sorting.
- `/course-view`: applications for the selected employee, after the existing role and supervisor checks.

Each list defaults to 10 results. Users can select 10, 20 or 25, then press Search. Searching or changing the size starts at page 1. The navigation preserves the keyword, size and selected employee. Manager review submissions also return to the same search page.

Search runs in the database before pagination. It matches course title, category, provider or stored status; the team list also matches employee name. Keywords are case-insensitive. `%` and `_` are treated as literal characters. Status values include APPLIED, UPDATED, APPROVED, REJECTED, DELETED, CANCELLED and COMPLETED.

Out-of-range numeric pages return to the last available page. Negative pages use page 1; unsupported sizes use 10. Non-numeric page or size parameters return HTTP 400. A page with no matches keeps its search form so the user can reset the search.

## Code map

All paths below are relative to the CATS project directory.

| File | Responsibility |
| --- | --- |
| `src/main/java/com/group5/cats/dto/ApplicationSearch.java` | Holds and normalizes keyword, page and size. |
| `src/main/java/com/group5/cats/dto/ApplicationPagination.java` | Converts the result into human-readable page numbers and result ranges. |
| `src/main/java/com/group5/cats/repository/CourseApplicationRepository.java` | Queries only the selected employee or manager's team, using Spring Data `Pageable`. |
| `src/main/java/com/group5/cats/service/CourseApplicationService.java` | Declares the two paginated search methods. |
| `src/main/java/com/group5/cats/service/CourseApplicationServiceImpl.java` | Applies year boundaries, stable sorting and page bounds. |
| `src/main/java/com/group5/cats/controller/EmployeeController.java` | Connects personal history to paginated search. |
| `src/main/java/com/group5/cats/controller/ManagerController.java` | Connects team search and preserves the return page after review. |
| `src/main/java/com/group5/cats/controller/CourseViewController.java` | Checks access to the selected employee before searching their applications. |
| `src/main/resources/templates/fragments/pagination.html` | Shared search form, page size selector, range and navigation links. |
| `src/main/resources/templates/my-history.html` | Personal application table. |
| `src/main/resources/templates/manager-approvals.html` | Team applications and review forms. |
| `src/main/resources/templates/course-view.html` | Selected employee's application table. |
| `src/main/resources/static/css/cats.css` | Scoped search and pagination styles using the existing visual design. |

Existing full-list service methods remain available for home summaries, quotas, date overlap checks, reports and calendar calculations. The month calendar and course catalogue are not paginated by this feature. No database schema changes or real database test records are needed.

## Verification

- `ApplicationPaginationTests`: isolated H2 records, 35-result paging without duplicates, owner/year restrictions, all three sizes, search, literal wildcard characters and page bounds.
- `ApplicationPaginationControllerTests`: login/role checks, request binding, owner selection and return-to-page after manager review.
- `CourseViewControllerTests`: existing role matrix now uses paginated results.
- `PageRenderingTests`: real Thymeleaf rendering, shared controls and encoded query parameters.

To check manually, restart the application on port 8080 and open My applications or Team applications. Course view shows the same controls after selecting an employee. If there are fewer records than the chosen size, no page navigation is needed; the search and result count remain visible.
