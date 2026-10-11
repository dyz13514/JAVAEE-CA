-- One-time upgrade for the older Course table (category text, optional duration_days).
-- Also supports the partially upgraded catalogue with unused direct-field columns.
-- Stop CATS, export a full database backup, and select the intended database first.
-- All source tables are archived before changes. Do not run twice.

DELIMITER $$
CREATE PROCEDURE cats_migrate_legacy_course_selection()
BEGIN
    DECLARE next_fk VARCHAR(255);
    DECLARE bad_rows BIGINT DEFAULT 0;
    DECLARE original_course_count BIGINT DEFAULT 0;
    DECLARE original_catalogue_count BIGINT DEFAULT 0;
    DECLARE original_application_count BIGINT DEFAULT 0;

    IF DATABASE() IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Select the intended database first.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                   AND table_name = 'courses' AND column_name = 'category')
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                      AND table_name = 'common_courses' AND column_name = 'course_id') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected the older Course/category-text structure.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE()
               AND table_name IN ('archive_courses_before_selection', 'archive_common_courses_before_selection',
                                  'archive_applications_before_selection', 'archive_course_dates_before_selection',
                                  'cats_selection_courses_new', 'cats_selection_common_courses_new')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Migration archives already exist. Review the previous attempt.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
               AND table_name = 'course_applications' AND column_name = 'category_name_snapshot') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Applications were partly upgraded. Review before retrying.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.key_column_usage WHERE referenced_table_schema = DATABASE()
               AND referenced_table_name = 'courses' AND table_name NOT IN ('common_courses', 'course_applications', 'course_start_dates'))
       OR EXISTS (SELECT 1 FROM information_schema.key_column_usage WHERE referenced_table_schema = DATABASE()
                   AND referenced_table_name = 'common_courses') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected Course/catalogue dependencies require review.';
    END IF;
    SELECT COUNT(*) INTO bad_rows FROM courses c LEFT JOIN training_providers p ON p.id = c.provider_id
        WHERE p.id IS NULL OR c.title IS NULL OR TRIM(c.title) = '' OR c.category IS NULL
              OR TRIM(c.category) = '' OR c.fee < 0;
    IF bad_rows > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Older courses contain invalid titles, providers, categories or fees.';
    END IF;
    SELECT COUNT(*) INTO bad_rows FROM common_courses cc LEFT JOIN courses c ON c.id = cc.course_id WHERE c.id IS NULL;
    IF bad_rows > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Catalogue references missing courses. Review before migration.';
    END IF;
    SELECT COUNT(*) INTO bad_rows FROM (
        SELECT LOWER(TRIM(title)), LOWER(TRIM(category)), provider_id FROM courses
        GROUP BY LOWER(TRIM(title)), LOWER(TRIM(category)), provider_id HAVING COUNT(*) > 1
    ) duplicates;
    IF bad_rows > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate course identities require review.';
    END IF;
    SELECT COUNT(*) INTO original_course_count FROM courses;
    SELECT COUNT(*) INTO original_catalogue_count FROM common_courses;
    SELECT COUNT(*) INTO original_application_count FROM course_applications;
    CREATE TABLE archive_courses_before_selection AS SELECT * FROM courses;
    CREATE TABLE archive_common_courses_before_selection AS SELECT * FROM common_courses;
    CREATE TABLE archive_applications_before_selection AS SELECT * FROM course_applications;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'course_start_dates') THEN
        CREATE TABLE archive_course_dates_before_selection AS SELECT * FROM course_start_dates;
    END IF;

    CREATE TABLE IF NOT EXISTS course_categories (
        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL UNIQUE,
        description VARCHAR(1000), half_day_allowed BIT NOT NULL
    );
    INSERT INTO course_categories (name, description, half_day_allowed)
        SELECT DISTINCT UPPER(TRIM(c.category)), 'Migrated course category', (UPPER(TRIM(c.category)) = 'INTERNAL')
        FROM courses c WHERE NOT EXISTS (SELECT 1 FROM course_categories cat WHERE UPPER(cat.name) = UPPER(TRIM(c.category)));
    INSERT INTO course_categories (name, description, half_day_allowed)
        SELECT DISTINCT UPPER(TRIM(a.category)), 'Migrated application category', (UPPER(TRIM(a.category)) = 'INTERNAL')
        FROM course_applications a WHERE a.category IS NOT NULL AND TRIM(a.category) <> ''
        AND NOT EXISTS (SELECT 1 FROM course_categories cat WHERE UPPER(cat.name) = UPPER(TRIM(a.category)));
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                   AND table_name = 'course_applications' AND column_name = 'category_id') THEN
        ALTER TABLE course_applications ADD COLUMN category_id BIGINT NULL;
    END IF;
    -- Keep already populated category IDs, including the mixed-schema case.
    UPDATE course_applications a JOIN course_categories cat ON UPPER(cat.name) = UPPER(TRIM(a.category))
        SET a.category_id = cat.id WHERE a.category_id IS NULL;
    IF EXISTS (SELECT 1 FROM course_applications a LEFT JOIN course_categories cat ON cat.id = a.category_id WHERE cat.id IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'An application category cannot be resolved. Source records are archived.';
    END IF;

    CREATE TABLE cats_selection_courses_new (
        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, title VARCHAR(255) NOT NULL,
        category_id BIGINT NOT NULL, provider_id BIGINT NOT NULL, fee DOUBLE NOT NULL,
        introduction VARCHAR(4000), active BIT NOT NULL DEFAULT b'1',
        CONSTRAINT uk_course_identity UNIQUE (title, category_id, provider_id),
        CONSTRAINT fk_selection_course_category FOREIGN KEY (category_id) REFERENCES course_categories(id),
        CONSTRAINT fk_selection_course_provider FOREIGN KEY (provider_id) REFERENCES training_providers(id)
    );
    INSERT INTO cats_selection_courses_new (id, title, category_id, provider_id, fee, introduction, active)
        SELECT c.id, TRIM(c.title), cat.id, c.provider_id,
               CASE WHEN cat.name = 'INTERNAL' THEN 0 ELSE c.fee END, c.introduction,
               EXISTS (SELECT 1 FROM common_courses cc WHERE cc.course_id = c.id)
        FROM courses c JOIN course_categories cat ON UPPER(cat.name) = UPPER(TRIM(c.category));
    CREATE TABLE cats_selection_common_courses_new (
        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, course_id BIGINT NOT NULL UNIQUE
    );
    INSERT INTO cats_selection_common_courses_new (id, course_id) SELECT id, course_id FROM common_courses;

    -- Application Course IDs keep their original meaning because Course IDs are preserved.
    SET next_fk = (SELECT constraint_name FROM information_schema.key_column_usage
        WHERE table_schema = DATABASE() AND table_name = 'course_applications'
              AND column_name = 'course_id' AND referenced_table_name IS NOT NULL LIMIT 1);
    WHILE next_fk IS NOT NULL DO
        SET @cats_selection_sql = CONCAT('ALTER TABLE course_applications DROP FOREIGN KEY `', REPLACE(next_fk, '`', '``'), '`');
        PREPARE cats_selection_statement FROM @cats_selection_sql;
        EXECUTE cats_selection_statement;
        DEALLOCATE PREPARE cats_selection_statement;
        SET next_fk = (SELECT constraint_name FROM information_schema.key_column_usage
            WHERE table_schema = DATABASE() AND table_name = 'course_applications'
                  AND column_name = 'course_id' AND referenced_table_name IS NOT NULL LIMIT 1);
    END WHILE;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                   AND table_name = 'course_applications' AND column_name = 'course_id') THEN
        ALTER TABLE course_applications ADD COLUMN course_id BIGINT NULL;
    END IF;
    DROP TABLE common_courses;
    DROP TABLE IF EXISTS course_start_dates;
    DROP TABLE courses;
    RENAME TABLE cats_selection_courses_new TO courses, cats_selection_common_courses_new TO common_courses;
    ALTER TABLE common_courses ADD CONSTRAINT fk_selection_common_course FOREIGN KEY (course_id) REFERENCES courses(id);
    ALTER TABLE course_applications ADD COLUMN category_name_snapshot VARCHAR(255) NULL,
        ADD CONSTRAINT fk_selection_application_course FOREIGN KEY (course_id) REFERENCES courses(id);
    UPDATE course_applications a JOIN course_categories cat ON cat.id = a.category_id SET a.category_name_snapshot = cat.name;
    -- The obsolete text category column is retained as nullable legacy data.
    ALTER TABLE course_applications MODIFY COLUMN category VARCHAR(255) NULL, MODIFY COLUMN category_id BIGINT NOT NULL;

    SELECT original_course_count AS courses_before, (SELECT COUNT(*) FROM courses) AS courses_after,
           original_catalogue_count AS catalogue_before, (SELECT COUNT(*) FROM common_courses) AS catalogue_after,
           original_application_count AS applications_before, (SELECT COUNT(*) FROM course_applications) AS applications_after;
END$$
DELIMITER ;

CALL cats_migrate_legacy_course_selection();
DROP PROCEDURE cats_migrate_legacy_course_selection;
