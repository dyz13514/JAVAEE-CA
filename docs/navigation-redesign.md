# CA1 navigation redesign

Preserve all existing business operations, role restrictions and URLs. The sidebar has five primary entries for staff, seven for managers and six for administrators. It has no third-level menus.

- Staff: Overview, Course catalogue, My training, My claims, Allowances & ledger.
- Managers: the same personal entries plus Approval centre and Team training.
- Administrators: Overview, Employees, Course management, Allowances & claims, Training records, System settings.

Administrator secondary links group courses/categories/providers, allowances/payments/ledger and holidays/email notifications. Clicking a parent opens its default working page; native disclosure controls expose other pages. The current group opens automatically.

Personal application lists and the legacy personal course view share one template. The all-years option preserves the legacy view's wider date range. List and calendar are views of My training. Course and claim reviews share an Approval centre, with a server-side review-state filter. Reports keep their two report types and CSV exports without nesting tab bars.

Create, edit, withdraw, cancel, feedback, claims, payments and allowance maintenance remain page actions. Approval and financial models stay distinct. Original access services continue to limit employee/team/admin data.

Validation: render real Thymeleaf pages for all roles and populated/empty states; verify primary-menu counts and administrator secondary-link coverage; verify pending/processed filtering before pagination, annual/all-years scope, personal/other employee permissions and retained exports; run the project test suite. Review desktop and mobile output.
