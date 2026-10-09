-- ONE-TIME upgrade from the old CommonCourse -> courses mapping.
-- Stop the application and back up the database first.
-- Run BEFORE adding new total courses. Do not rerun after editing the common catalogue.
-- Select the intended database in MySQL Workbench before running this script.
CREATE TABLE IF NOT EXISTS common_courses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_common_courses_course UNIQUE (course_id),
    CONSTRAINT fk_common_courses_course FOREIGN KEY (course_id) REFERENCES courses(id)
);

INSERT INTO common_courses (course_id)
SELECT c.id
FROM courses c
WHERE NOT EXISTS (
    SELECT 1 FROM common_courses cc WHERE cc.course_id = c.id
);

-- Hibernate adds the nullable course_applications.course_id on the next startup.
-- Existing applications retain their snapshots; no title-based automatic matching.
