# Course and CommonCourse

- `Course` maps to `courses`: all recorded course details, including provider and reference fee.
- `CommonCourse` maps to `common_courses`: one administrator-selected catalogue entry per course.
- `CourseApplication.course` maps to nullable `course_applications.course_id`.
- Dates and the actual requested fee belong to each application. Title, category and provider are copied into its snapshot fields.
- Existing/manual applications may have no course link. They are not automatically matched by title or promoted into the catalogue.
- DataLoader now creates six sample courses only when courses is empty. It reuses matching provider names and does not add any common_courses entries.

## Existing database upgrade

The old CommonCourse entity used the table name `courses`. Its rows and IDs are now used directly by Course; do not rename or drop that table.

If your existing `courses` table contains old commonly attended courses, stop the application, back up the database, and run `database/migrate-common-course-catalogue.sql` **once, before using the new version**. It preserves their common-catalogue membership. It does not delete or modify course data. Do not run it after adding new total courses: that would mark those courses as common too.

For a fresh database or an empty old course catalogue, simply start the application. The current Hibernate `ddl-auto=update` creates `common_courses` and adds the nullable application foreign key. Existing applications keep their original information and have a null course link.

## Manual checks

1. Admin → All courses → Add Course (add a provider first if needed).
2. Admin → Common course catalogue → Select Course.
3. Selecting the same course twice should show a validation message.
4. Removing a common entry must leave its total course available.
5. Staff → Apply for course → choose a recorded course, check the actual fee and submit.
6. Changing course/provider details must not rewrite saved application details.
7. A course referenced by a catalogue entry or application cannot be deleted.
8. A provider with any recorded course cannot be deleted.

The original manual-entry application option remains available for courses not yet recorded.

## Course identity and catalogue status

The create/edit form rejects the same title (ignoring case and outer spaces), category and provider combination. The same title may exist for different providers or categories. This is a service-level validation; the unique database constraint on common_courses.course_id separately guarantees one common entry per course.

All courses shows a Commonly Attended column calculated from common_courses. There is no second boolean flag to keep in sync.

## Browsing and searching

Staff and managers can open Browse courses from the sidebar. Staff can follow Apply to prefill a new application; that GET request does not save anything. The final submission still validates the selected course on the server. Existing/manual applications remain supported.

Both /courses and /admin/courses support keyword (course or provider name), providerId, category and commonOnly filters. Filters combine with AND. Keyword matching ignores case and outer spaces. Reset filters shows all courses again. The implementation deliberately uses a simple loop suitable for the current small catalogue.

## Sample courses and card layout

On the next normal startup, an empty course table receives Java/Spring Boot, SQL, Cloud Architecture, Workplace Communication, Security Awareness and Project Management Certification samples. Their providers and fees are demonstration data, not verified commercial offerings. If the table already contains a course, the loader leaves the catalogue unchanged. If all courses are deleted, the next startup seeds it again.

The employee page shows the administrator-selected Commonly Attended Courses first, independently of filters. Course overview below includes all courses by default, including common courses, and supports the existing search filters. Admin management retains its table layout. No completion percentages are displayed because catalogue entries are not enrolment records.
