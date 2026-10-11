-- Optional demonstration data for an upgraded database. Does not overwrite existing courses.
-- 18 concrete variants, 6 distinct titles; INTERNAL is free, others cost 200/300/500.
START TRANSACTION;
INSERT INTO training_providers (name)
    SELECT 'ISS' WHERE NOT EXISTS (SELECT 1 FROM training_providers WHERE LOWER(name) = 'iss');
INSERT INTO training_providers (name)
    SELECT 'AWS' WHERE NOT EXISTS (SELECT 1 FROM training_providers WHERE LOWER(name) = 'aws');
INSERT INTO training_providers (name)
    SELECT 'Demo Skills Academy' WHERE NOT EXISTS (SELECT 1 FROM training_providers WHERE LOWER(name) = 'demo skills academy');

INSERT INTO courses (title, category_id, provider_id, fee, introduction, active)
SELECT sample.title, cat.id, p.id, sample.fee,
       CONCAT('A practical introduction to ', sample.title, '. Learn through guided exercises.'), b'1'
FROM (
    SELECT 'Java' title, 'INTERNAL' category, 'ISS' provider, 0 fee
    UNION ALL SELECT 'Java', 'EXTERNAL', 'AWS', 500
    UNION ALL SELECT 'Java', 'CERTIFICATION', 'Demo Skills Academy', 500
    UNION ALL SELECT 'Python', 'INTERNAL', 'ISS', 0
    UNION ALL SELECT 'Python', 'EXTERNAL', 'Demo Skills Academy', 200
    UNION ALL SELECT 'Python', 'CERTIFICATION', 'Demo Skills Academy', 300
    UNION ALL SELECT 'SQL', 'INTERNAL', 'ISS', 0
    UNION ALL SELECT 'SQL', 'EXTERNAL', 'Demo Skills Academy', 200
    UNION ALL SELECT 'SQL', 'CERTIFICATION', 'Demo Skills Academy', 300
    UNION ALL SELECT 'Cloud Architecture', 'INTERNAL', 'ISS', 0
    UNION ALL SELECT 'Cloud Architecture', 'EXTERNAL', 'AWS', 500
    UNION ALL SELECT 'Cloud Architecture', 'CERTIFICATION', 'Demo Skills Academy', 300
    UNION ALL SELECT 'Cybersecurity', 'INTERNAL', 'ISS', 0
    UNION ALL SELECT 'Cybersecurity', 'EXTERNAL', 'Demo Skills Academy', 300
    UNION ALL SELECT 'Cybersecurity', 'CERTIFICATION', 'Demo Skills Academy', 200
    UNION ALL SELECT 'Project Management', 'INTERNAL', 'ISS', 0
    UNION ALL SELECT 'Project Management', 'EXTERNAL', 'Demo Skills Academy', 300
    UNION ALL SELECT 'Project Management', 'CERTIFICATION', 'Demo Skills Academy', 200
) sample JOIN course_categories cat ON cat.name = sample.category
         JOIN training_providers p ON LOWER(p.name) = LOWER(sample.provider)
WHERE NOT EXISTS (SELECT 1 FROM courses c WHERE LOWER(c.title) = LOWER(sample.title)
                  AND c.category_id = cat.id AND c.provider_id = p.id);

-- Common catalogue membership is selected manually by an administrator.
COMMIT;
