-- ONE-TIME MySQL 8 upgrade: application provider name -> provider_id.
-- Stop Spring Boot, export a full database backup and select the intended schema.
-- Fresh databases do not need this script. Run the entire file including DELIMITER.
-- Blank legacy names require an administrator to identify the actual provider first.
-- MySQL DDL commits automatically; a full backup is needed for recovery.

DELIMITER $$
CREATE PROCEDURE cats_migrate_application_provider()
BEGIN
    DECLARE bad_rows BIGINT DEFAULT 0;

    IF DATABASE() IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Select the intended database first.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'course_applications'
                     AND column_name = 'provider' AND data_type IN ('varchar', 'text'))
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'training_providers') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected legacy provider name column and training_providers table. Do not rerun after migration.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE()
               AND table_name IN ('archive_applications_before_provider', 'archive_providers_before_provider')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Provider migration archives already exist. Inspect the previous attempt before retrying.';
    END IF;

    SELECT COUNT(*) INTO bad_rows FROM course_applications
        WHERE provider IS NULL OR TRIM(provider) = '';
    IF bad_rows > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Some applications have no provider name. Identify and fill their actual provider before migration.';
    END IF;
    SELECT COUNT(*) INTO bad_rows FROM (
        SELECT UPPER(TRIM(name)) AS normalised_name FROM training_providers
        GROUP BY UPPER(TRIM(name)) HAVING COUNT(*) > 1
    ) duplicate_names;
    IF bad_rows > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Provider names match ambiguously after trimming and case folding. Resolve duplicates first.';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
               AND table_name = 'course_applications' AND column_name = 'provider_id') THEN
        SELECT COUNT(*) INTO bad_rows FROM course_applications a
            LEFT JOIN training_providers p ON p.id = a.provider_id
            WHERE a.provider_id IS NOT NULL
              AND (p.id IS NULL OR UPPER(TRIM(p.name)) <> UPPER(TRIM(a.provider)));
        IF bad_rows > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Existing provider_id values disagree with legacy names. Resolve these before migration.';
        END IF;
    END IF;

    CREATE TABLE archive_applications_before_provider AS SELECT * FROM course_applications;
    CREATE TABLE archive_providers_before_provider AS SELECT * FROM training_providers;

    -- Preserve existing provider IDs. Create catalogue entries for nonblank free-text names.
    INSERT INTO training_providers (name)
        SELECT MIN(TRIM(a.provider)) FROM course_applications a
        WHERE NOT EXISTS (
            SELECT 1 FROM training_providers p WHERE UPPER(TRIM(p.name)) = UPPER(TRIM(a.provider))
        )
        GROUP BY UPPER(TRIM(a.provider));

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                   AND table_name = 'course_applications' AND column_name = 'provider_id') THEN
        ALTER TABLE course_applications ADD COLUMN provider_id BIGINT NULL;
    END IF;

    UPDATE course_applications a JOIN training_providers p
        ON UPPER(TRIM(p.name)) = UPPER(TRIM(a.provider))
        SET a.provider_id = p.id WHERE a.provider_id IS NULL;

    SELECT COUNT(*) INTO bad_rows FROM course_applications a
        LEFT JOIN training_providers p ON p.id = a.provider_id WHERE p.id IS NULL;
    IF bad_rows > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Some applications could not be linked. Keep legacy names and restore from the backup if necessary.';
    END IF;

    ALTER TABLE course_applications MODIFY COLUMN provider_id BIGINT NOT NULL;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.key_column_usage
        WHERE table_schema = DATABASE() AND table_name = 'course_applications'
          AND column_name = 'provider_id' AND referenced_table_name = 'training_providers'
          AND referenced_column_name = 'id'
    ) THEN
        ALTER TABLE course_applications ADD CONSTRAINT fk_application_training_provider
            FOREIGN KEY (provider_id) REFERENCES training_providers(id);
    END IF;
    ALTER TABLE course_applications DROP COLUMN provider;
END$$
DELIMITER ;

CALL cats_migrate_application_provider();
DROP PROCEDURE cats_migrate_application_provider;

-- Fees, training days, statuses, employee IDs and application IDs are preserved.
SELECT (SELECT COUNT(*) FROM archive_applications_before_provider) AS old_application_rows,
       (SELECT COUNT(*) FROM course_applications) AS new_application_rows;
SELECT a.id AS application_id, a.provider_id, p.name AS provider_name
    FROM course_applications a JOIN training_providers p ON p.id = a.provider_id ORDER BY a.id;
