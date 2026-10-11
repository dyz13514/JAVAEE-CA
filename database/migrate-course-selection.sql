-- One-time MySQL upgrade from the direct-field CommonCourse catalogue.
-- Stop CATS, export a full database backup, and select the intended schema first.
-- Empty databases are created and seeded by Spring Boot; do not run this there.
-- This keeps original catalogue/application rows in archive tables.
-- MySQL DDL auto-commits. If interrupted, inspect the archives before retrying.

DELIMITER $$
CREATE PROCEDURE cats_migrate_course_selection()
BEGIN
    DECLARE bad_rows BIGINT DEFAULT 0;
    DECLARE original_catalogue_count BIGINT DEFAULT 0;
    DECLARE original_application_count BIGINT DEFAULT 0;

    IF DATABASE() IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Select the intended database first.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE()
               AND table_name IN ('courses', 'archive_common_courses_before_selection',
                                  'archive_applications_before_selection', 'cats_selection_common_courses_new')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected the direct-field catalogue. Existing Course or migration archives need review; do not rerun.';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'common_courses' AND column_name IN ('title', 'category_id', 'provider_id', 'fee', 'introduction')) <> 5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Upgrade to the Category/direct-field catalogue before running this script.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
               AND table_name = 'course_applications' AND column_name IN ('course_id', 'category_name_snapshot')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Application schema was partly upgraded. Review the previous attempt before migration.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.key_column_usage WHERE referenced_table_schema = DATABASE()
               AND referenced_table_name = 'common_courses') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Another table references the catalogue. Review that dependency before migration.';
    END IF;
    SELECT COUNT(*) INTO bad_rows FROM common_courses cc
        LEFT JOIN course_categories cat ON cat.id = cc.category_id
        LEFT JOIN training_providers p ON p.id = cc.provider_id
        WHERE cat.id IS NULL OR p.id IS NULL OR cc.title IS NULL OR TRIM(cc.title) = ''
              OR cc.fee IS NULL OR cc.fee < 0;
    IF bad_rows > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Catalogue has invalid titles, categories, providers or fees. Fix them first.';
    END IF;
    SELECT COUNT(*) INTO bad_rows FROM (
        SELECT LOWER(TRIM(title)), category_id, provider_id FROM common_courses
        GROUP BY LOWER(TRIM(title)), category_id, provider_id HAVING COUNT(*) > 1
    ) duplicate_courses;
    IF bad_rows > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate course name/category/provider combinations require review.';
    END IF;
    SELECT COUNT(*) INTO original_catalogue_count FROM common_courses;
    SELECT COUNT(*) INTO original_application_count FROM course_applications;
    CREATE TABLE archive_applications_before_selection AS SELECT * FROM course_applications;

    CREATE TABLE courses (
        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        title VARCHAR(255) NOT NULL,
        category_id BIGINT NOT NULL,
        provider_id BIGINT NOT NULL,
        fee DOUBLE NOT NULL,
        introduction VARCHAR(4000),
        active BIT NOT NULL DEFAULT b'1',
        CONSTRAINT uk_course_identity UNIQUE (title, category_id, provider_id),
        CONSTRAINT fk_selection_course_category FOREIGN KEY (category_id) REFERENCES course_categories(id),
        CONSTRAINT fk_selection_course_provider FOREIGN KEY (provider_id) REFERENCES training_providers(id)
    );
    INSERT INTO courses (id, title, category_id, provider_id, fee, introduction, active)
        SELECT cc.id, TRIM(cc.title), cc.category_id, cc.provider_id,
               CASE WHEN cat.name = 'INTERNAL' THEN 0 ELSE cc.fee END, cc.introduction, b'1'
        FROM common_courses cc JOIN course_categories cat ON cat.id = cc.category_id;

    CREATE TABLE cats_selection_common_courses_new (
        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        course_id BIGINT NOT NULL UNIQUE,
        CONSTRAINT fk_selection_common_course FOREIGN KEY (course_id) REFERENCES courses(id)
    );
    INSERT INTO cats_selection_common_courses_new (id, course_id) SELECT id, id FROM common_courses;
    RENAME TABLE common_courses TO archive_common_courses_before_selection,
                 cats_selection_common_courses_new TO common_courses;

    ALTER TABLE course_applications ADD COLUMN course_id BIGINT NULL,
        ADD COLUMN category_name_snapshot VARCHAR(255) NULL,
        ADD CONSTRAINT fk_selection_application_course FOREIGN KEY (course_id) REFERENCES courses(id);
    UPDATE course_applications a JOIN course_categories cat ON cat.id = a.category_id
        SET a.category_name_snapshot = cat.name;
    -- Link only exact historical matches. Never replace historical names, fees or dates.
    UPDATE course_applications a JOIN courses c
        ON LOWER(TRIM(a.course_title)) = LOWER(c.title) AND a.category_id = c.category_id AND a.fee = c.fee
        JOIN training_providers p ON p.id = c.provider_id AND LOWER(TRIM(a.provider)) = LOWER(p.name)
        SET a.course_id = c.id;

    SELECT original_catalogue_count AS catalogue_before, (SELECT COUNT(*) FROM common_courses) AS catalogue_after,
           original_application_count AS applications_before, (SELECT COUNT(*) FROM course_applications) AS applications_after;
    SELECT COUNT(*) AS legacy_applications_without_course FROM course_applications WHERE course_id IS NULL;
END$$
DELIMITER ;

CALL cats_migrate_course_selection();
DROP PROCEDURE cats_migrate_course_selection;
