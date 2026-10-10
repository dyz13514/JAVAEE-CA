-- ONE-TIME MySQL 8 upgrade from Course + CommonCourse(course_id).
-- Stop Spring Boot, export a full database backup, and select the intended schema first.
-- Fresh databases do not need this script. Run the entire file, including DELIMITER.
-- Old records are also copied into archive_* tables without foreign keys.
-- DDL in MySQL commits automatically: use the full export to recover if migration is interrupted.

DELIMITER $$
CREATE PROCEDURE cats_migrate_category_catalogue()
BEGIN
    DECLARE bad_rows BIGINT DEFAULT 0;
    DECLARE next_fk VARCHAR(255);

    IF DATABASE() IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Select the intended database first.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = DATABASE() AND table_name = 'courses')
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns
                      WHERE table_schema = DATABASE() AND table_name = 'common_courses' AND column_name = 'course_id') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected old Course/common_courses structure. Do not rerun on an upgraded database.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE()
               AND table_name IN ('archive_courses_before_category', 'archive_common_courses_before_category',
                                  'archive_applications_before_category', 'archive_course_dates_before_category', 'cats_migration_common_courses_new',
                                  'cats_migration_common_courses_old')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Migration archives already exist. Inspect the previous attempt; do not overwrite backups.';
    END IF;

    SELECT COUNT(*) INTO bad_rows FROM common_courses cc LEFT JOIN courses c ON c.id = cc.course_id
        LEFT JOIN training_providers p ON p.id = c.provider_id
        WHERE c.id IS NULL OR p.id IS NULL OR c.title IS NULL
            OR c.category IS NULL OR TRIM(c.category) = '' OR UPPER(TRIM(c.category)) = 'ALL';
    IF bad_rows > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Catalogue contains missing courses/providers or invalid categories. Fix these before migration.';
    END IF;
    SELECT COUNT(*) INTO bad_rows FROM course_applications
        WHERE category IS NULL OR TRIM(category) = '' OR UPPER(TRIM(category)) = 'ALL';
    IF bad_rows > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Applications have blank/reserved categories. Review them before migration.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.key_column_usage
               WHERE referenced_table_schema = DATABASE() AND referenced_table_name = 'courses'
                 AND table_name NOT IN ('common_courses', 'course_applications', 'course_start_dates')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'An unexpected table references courses. Review that dependency before migration.';
    END IF;

    CREATE TABLE archive_courses_before_category AS
        SELECT c.*, p.name AS provider_name_snapshot FROM courses c LEFT JOIN training_providers p ON p.id = c.provider_id;
    CREATE TABLE archive_common_courses_before_category AS SELECT * FROM common_courses;
    CREATE TABLE archive_applications_before_category AS SELECT * FROM course_applications;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'course_start_dates') THEN
        CREATE TABLE archive_course_dates_before_category AS SELECT * FROM course_start_dates;
    END IF;

    CREATE TABLE IF NOT EXISTS course_categories (
        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(255) NOT NULL UNIQUE,
        description VARCHAR(1000),
        half_day_allowed BIT NOT NULL
    );
    INSERT INTO course_categories (name, description, half_day_allowed)
        SELECT 'INTERNAL', 'Internal training', b'1' FROM DUAL
        WHERE NOT EXISTS (SELECT 1 FROM course_categories WHERE UPPER(name) = 'INTERNAL');
    INSERT INTO course_categories (name, description, half_day_allowed)
        SELECT 'EXTERNAL', 'External course', b'0' FROM DUAL
        WHERE NOT EXISTS (SELECT 1 FROM course_categories WHERE UPPER(name) = 'EXTERNAL');
    INSERT INTO course_categories (name, description, half_day_allowed)
        SELECT 'CERTIFICATION', 'Professional certification', b'0' FROM DUAL
        WHERE NOT EXISTS (SELECT 1 FROM course_categories WHERE UPPER(name) = 'CERTIFICATION');
    -- Preserve any legacy custom names; original three retain their fixed settings.
    INSERT INTO course_categories (name, description, half_day_allowed)
        SELECT DISTINCT TRIM(a.category), 'Migrated application category', b'0'
        FROM course_applications a WHERE NOT EXISTS
            (SELECT 1 FROM course_categories cat WHERE UPPER(cat.name) = UPPER(TRIM(a.category)));
    INSERT INTO course_categories (name, description, half_day_allowed)
        SELECT DISTINCT TRIM(c.category), 'Migrated catalogue category', b'0'
        FROM courses c JOIN common_courses cc ON cc.course_id = c.id WHERE NOT EXISTS
            (SELECT 1 FROM course_categories cat WHERE UPPER(cat.name) = UPPER(TRIM(c.category)));
    UPDATE course_categories SET name = UPPER(name), half_day_allowed = (UPPER(name) = 'INTERNAL')
        WHERE UPPER(name) IN ('INTERNAL', 'EXTERNAL', 'CERTIFICATION');

    CREATE TABLE cats_migration_common_courses_new (
        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        title VARCHAR(255) NOT NULL,
        category_id BIGINT NOT NULL,
        provider_id BIGINT NOT NULL,
        fee DOUBLE NOT NULL,
        introduction VARCHAR(4000),
        CONSTRAINT fk_common_category FOREIGN KEY (category_id) REFERENCES course_categories(id),
        CONSTRAINT fk_common_provider FOREIGN KEY (provider_id) REFERENCES training_providers(id)
    );
    INSERT INTO cats_migration_common_courses_new (id, title, category_id, provider_id, fee, introduction)
        SELECT cc.id, c.title, cat.id, c.provider_id, c.fee, c.introduction
        FROM common_courses cc JOIN courses c ON c.id = cc.course_id
        JOIN course_categories cat ON UPPER(cat.name) = UPPER(TRIM(c.category));
    IF (SELECT COUNT(*) FROM common_courses) <> (SELECT COUNT(*) FROM cats_migration_common_courses_new) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Catalogue row counts differ. Stop and inspect archives.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                   AND table_name = 'course_applications' AND column_name = 'category_id') THEN
        ALTER TABLE course_applications ADD COLUMN category_id BIGINT NULL;
    END IF;
    UPDATE course_applications a JOIN course_categories cat ON UPPER(cat.name) = UPPER(TRIM(a.category))
        SET a.category_id = cat.id;
    IF EXISTS (SELECT 1 FROM course_applications WHERE category_id IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Some application categories could not be mapped. Stop and inspect archives.';
    END IF;
    ALTER TABLE course_applications MODIFY COLUMN category_id BIGINT NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM information_schema.key_column_usage WHERE table_schema = DATABASE()
                   AND table_name = 'course_applications' AND column_name = 'category_id'
                   AND referenced_table_name = 'course_categories') THEN
        ALTER TABLE course_applications ADD CONSTRAINT fk_application_category
            FOREIGN KEY (category_id) REFERENCES course_categories(id);
    END IF;

    -- Remove only obsolete application -> Course foreign keys, whatever Hibernate named them.
    SET next_fk = (SELECT constraint_name FROM information_schema.key_column_usage
                   WHERE table_schema = DATABASE() AND table_name = 'course_applications'
                     AND column_name = 'course_id' AND referenced_table_name IS NOT NULL LIMIT 1);
    WHILE next_fk IS NOT NULL DO
        SET @cats_migration_sql = CONCAT('ALTER TABLE course_applications DROP FOREIGN KEY `', REPLACE(next_fk, '`', '``'), '`');
        PREPARE cats_statement FROM @cats_migration_sql;
        EXECUTE cats_statement;
        DEALLOCATE PREPARE cats_statement;
        SET next_fk = (SELECT constraint_name FROM information_schema.key_column_usage
                       WHERE table_schema = DATABASE() AND table_name = 'course_applications'
                         AND column_name = 'course_id' AND referenced_table_name IS NOT NULL LIMIT 1);
    END WHILE;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
               AND table_name = 'course_applications' AND column_name = 'course_id') THEN
        ALTER TABLE course_applications DROP COLUMN course_id;
    END IF;
    ALTER TABLE course_applications DROP COLUMN category;
    RENAME TABLE common_courses TO cats_migration_common_courses_old,
                 cats_migration_common_courses_new TO common_courses;
    DROP TABLE cats_migration_common_courses_old;
    DROP TABLE IF EXISTS course_start_dates;
    DROP TABLE courses;
END$$
DELIMITER ;

CALL cats_migrate_category_catalogue();
DROP PROCEDURE cats_migrate_category_catalogue;

-- Application fees, dates, training_days, statuses and all employee IDs are preserved.
-- Unselected old courses and old schedules remain in archive_* tables.
SELECT name, half_day_allowed FROM course_categories ORDER BY name;
SELECT (SELECT COUNT(*) FROM archive_common_courses_before_category) AS old_catalogue_rows,
       (SELECT COUNT(*) FROM common_courses) AS new_catalogue_rows,
       (SELECT COUNT(*) FROM archive_applications_before_category) AS old_application_rows,
       (SELECT COUNT(*) FROM course_applications) AS new_application_rows;
