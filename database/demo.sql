-- CATS local demo database, matching the current JPA models.
-- Import once into a new cats_demo database. Existing tables cause an error;
-- this script never drops tables or deletes an existing business database.
CREATE DATABASE IF NOT EXISTS cats_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE cats_demo;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `annual_entitlements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `entitlement_year` int DEFAULT NULL,
  `training_budget` double NOT NULL,
  `training_days_limit` double NOT NULL,
  `employee_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entitlement_employee_year` (`employee_id`,`entitlement_year`),
  CONSTRAINT `FKb7lvus0p8xnji6cdtakyx0rc4` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `claim_attachments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content_type` varchar(40) NOT NULL,
  `file_size` bigint NOT NULL,
  `original_filename` varchar(180) NOT NULL,
  `stored_filename` varchar(80) NOT NULL,
  `uploaded_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1j14aa1u4t7b2bf6q9yp5njtm` (`stored_filename`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `claim_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint NOT NULL,
  `actor_name` varchar(180) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `comment` varchar(1000) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `status` enum('APPROVED','PENDING','REIMBURSED','REJECTED') NOT NULL,
  `certificate_id` bigint NOT NULL,
  `claim_id` bigint NOT NULL,
  `receipt_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK2fab8fn9vxohlr2l8v3hwknoa` (`certificate_id`),
  KEY `FKjkxx7nkrset6llwolslfgh43g` (`claim_id`),
  KEY `FKiporucxrgilgaxojj7ggr4mdh` (`receipt_id`),
  CONSTRAINT `FK2fab8fn9vxohlr2l8v3hwknoa` FOREIGN KEY (`certificate_id`) REFERENCES `claim_attachments` (`id`),
  CONSTRAINT `FKiporucxrgilgaxojj7ggr4mdh` FOREIGN KEY (`receipt_id`) REFERENCES `claim_attachments` (`id`),
  CONSTRAINT `FKjkxx7nkrset6llwolslfgh43g` FOREIGN KEY (`claim_id`) REFERENCES `course_fee_claims` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `common_courses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fee` double NOT NULL,
  `introduction` varchar(4000) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `category_id` bigint NOT NULL,
  `provider_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8f1py72fdr3di8xhamebdexo9` (`category_id`),
  KEY `FKmknd6kbrckm48v9529vil4q9u` (`provider_id`),
  CONSTRAINT `FK8f1py72fdr3di8xhamebdexo9` FOREIGN KEY (`category_id`) REFERENCES `course_categories` (`id`),
  CONSTRAINT `FKmknd6kbrckm48v9529vil4q9u` FOREIGN KEY (`provider_id`) REFERENCES `training_providers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_applications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_title` varchar(255) DEFAULT NULL,
  `dissemination` varchar(255) DEFAULT NULL,
  `experience_comments` varchar(255) DEFAULT NULL,
  `fee` double NOT NULL,
  `from_date` date DEFAULT NULL,
  `half_day` bit(1) DEFAULT NULL,
  `justification` varchar(255) DEFAULT NULL,
  `manager_comment` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `to_date` date DEFAULT NULL,
  `training_days` double NOT NULL,
  `category_id` bigint NOT NULL,
  `employee_id` bigint DEFAULT NULL,
  `provider_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK5tsg0lhkckd72n8imenoc6i7b` (`category_id`),
  KEY `FKp1l42mr3oy2desj2bhdppwn9g` (`employee_id`),
  KEY `FK44j9mi3dyjupcvsjofe4gevvs` (`provider_id`),
  CONSTRAINT `FK44j9mi3dyjupcvsjofe4gevvs` FOREIGN KEY (`provider_id`) REFERENCES `training_providers` (`id`),
  CONSTRAINT `FK5tsg0lhkckd72n8imenoc6i7b` FOREIGN KEY (`category_id`) REFERENCES `course_categories` (`id`),
  CONSTRAINT `FKp1l42mr3oy2desj2bhdppwn9g` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(1000) DEFAULT NULL,
  `half_day_allowed` bit(1) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnxksnnnkvgo1vca6ffhyfrpux` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_fee_claims` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(12,2) NOT NULL,
  `manager_reason` varchar(1000) DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `paid_by_id` bigint DEFAULT NULL,
  `paid_by_name` varchar(180) DEFAULT NULL,
  `paid_personally` bit(1) NOT NULL,
  `payment_date` date DEFAULT NULL,
  `payment_note` varchar(1000) DEFAULT NULL,
  `payment_reference` varchar(100) DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `reviewed_by_id` bigint DEFAULT NULL,
  `reviewed_by_name` varchar(180) DEFAULT NULL,
  `status` enum('APPROVED','PENDING','REIMBURSED','REJECTED') NOT NULL,
  `submitted_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `certificate_id` bigint NOT NULL,
  `receipt_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_claim_application` (`application_id`),
  KEY `FKbnh6q7duw5u9pwdc65tr633gq` (`certificate_id`),
  KEY `FKcsttpn6f9xod6nuyve3lopuiw` (`receipt_id`),
  CONSTRAINT `FK27t1evgasw7diui81ujhs0so9` FOREIGN KEY (`application_id`) REFERENCES `course_applications` (`id`),
  CONSTRAINT `FKbnh6q7duw5u9pwdc65tr633gq` FOREIGN KEY (`certificate_id`) REFERENCES `claim_attachments` (`id`),
  CONSTRAINT `FKcsttpn6f9xod6nuyve3lopuiw` FOREIGN KEY (`receipt_id`) REFERENCES `claim_attachments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employees` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `designation` enum('ADMINISTRATIVE','PROFESSIONAL') DEFAULT NULL,
  `email` varchar(254) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `role` enum('ADMIN','MANAGER','REGULAR_STAFF') DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `supervisor_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK2q9drrvhye5m5ivk5h6re2b4i` (`supervisor_id`),
  CONSTRAINT `FK2q9drrvhye5m5ivk5h6re2b4i` FOREIGN KEY (`supervisor_id`) REFERENCES `employees` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `body` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deduplication_key` varchar(150) NOT NULL,
  `delivery_status` enum('FAILED','PENDING','PROCESSING','SENT') NOT NULL,
  `last_error` text,
  `next_attempt_at` datetime(6) DEFAULT NULL,
  `notification_type` enum('APPLICATION_APPROVED','APPLICATION_REJECTED','APPLICATION_SUBMITTED') NOT NULL,
  `recipient_email_snapshot` varchar(254) DEFAULT NULL,
  `retry_count` int NOT NULL,
  `sent_at` datetime(6) DEFAULT NULL,
  `subject` varchar(255) NOT NULL,
  `application_id` bigint NOT NULL,
  `recipient_employee_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKqsipxaj6hotq24lq0jphf5phv` (`deduplication_key`),
  KEY `idx_notification_due` (`delivery_status`,`next_attempt_at`),
  KEY `FK1na6bifi9nxk9gix22u7x0g50` (`application_id`),
  KEY `FKo6kbypi03cbncciekmni1cvuw` (`recipient_employee_id`),
  CONSTRAINT `FK1na6bifi9nxk9gix22u7x0g50` FOREIGN KEY (`application_id`) REFERENCES `course_applications` (`id`),
  CONSTRAINT `FKo6kbypi03cbncciekmni1cvuw` FOREIGN KEY (`recipient_employee_id`) REFERENCES `employees` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `public_holidays` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `holiday_date` date NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKj92fsuoavrquc92at2ly6x223` (`holiday_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `registration_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `review_reason` varchar(300) DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `status` varchar(16) NOT NULL,
  `submitted_at` datetime(6) NOT NULL,
  `username` varchar(40) NOT NULL,
  `reviewed_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmj6qtagb4x4mh8d3f0d0n2sck` (`username`),
  KEY `FKa19jhuf4k0ovxl6usms0xmeln` (`reviewed_by_id`),
  CONSTRAINT `FKa19jhuf4k0ovxl6usms0xmeln` FOREIGN KEY (`reviewed_by_id`) REFERENCES `employees` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `training_providers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm35bn3av0tpt8rho3114e3xjr` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


-- Local demonstration data. Run after schema creation in an empty cats_demo database.
-- Dates are derived from the import date; future requests use separate working weeks.
START TRANSACTION;
SET @year = YEAR(CURDATE());
SET @monday = DATE_ADD(CURDATE(), INTERVAL (7 - WEEKDAY(CURDATE())) DAY);
SET @past = DATE_SUB(@monday, INTERVAL 42 DAY);
INSERT INTO course_categories (id,name,description,half_day_allowed) VALUES
(1,'INTERNAL','Training provided within the organisation.',b'1'),
(2,'EXTERNAL','External courses provided by training providers.',b'0'),
(3,'CERTIFICATION','Professional certification programmes.',b'0');
INSERT INTO training_providers (id,name) VALUES
(1,'CATS Internal Learning'),(2,'Demo Technology Academy'),(3,'Demo Professional Institute');
INSERT INTO employees (id,username,password,name,email,designation,role,supervisor_id) VALUES
(1,'admin','{bcrypt}$2a$10$bGwxGIdUFGJCk6UZwFoTqeIOrp7goURD.VJInAwCbvRym02d0mCVO','Demo Administrator',NULL,'ADMINISTRATIVE','ADMIN',NULL),
(2,'director1','{bcrypt}$2a$10$auuJ4/ewYeVl5YYv.x1l9uHc13rlriZbh4QibvWy.675USG183FDC','Jamie Chen',NULL,'PROFESSIONAL','MANAGER',NULL),
(3,'manager1','{bcrypt}$2a$10$.tugXG6iKLvmBixa03HoVORIauRIjME7LgC.YJyyouxIGTQGHuNbK','Alice Wong',NULL,'PROFESSIONAL','MANAGER',2),
(4,'emp1','{bcrypt}$2a$10$nCXLZ.hAMydY.L9jtJwmQOgktD.3sF3izw/DiJ6QVotDiMYMOPFAG','Ben Tan',NULL,'PROFESSIONAL','REGULAR_STAFF',3),
(5,'emp2','{bcrypt}$2a$10$nCXLZ.hAMydY.L9jtJwmQOgktD.3sF3izw/DiJ6QVotDiMYMOPFAG','Cathy Lim',NULL,'ADMINISTRATIVE','REGULAR_STAFF',3);
INSERT INTO annual_entitlements (employee_id,entitlement_year,training_budget,training_days_limit)
SELECT id,@year,20000,CASE WHEN designation='PROFESSIONAL' THEN 100 ELSE 50 END FROM employees;
INSERT INTO annual_entitlements (employee_id,entitlement_year,training_budget,training_days_limit)
SELECT id,YEAR(@past),20000,CASE WHEN designation='PROFESSIONAL' THEN 100 ELSE 50 END FROM employees
WHERE YEAR(@past) <> @year;
INSERT INTO course_applications
(id,employee_id,category_id,provider_id,course_title,from_date,to_date,fee,training_days,half_day,status,justification,dissemination,manager_comment,experience_comments) VALUES
(1,4,2,2,'Practical SQL for Application Developers',@monday,DATE_ADD(@monday,INTERVAL 2 DAY),480,3,b'0','APPLIED','Improve database query design for current projects.','Share examples in a team session.',NULL,NULL),
(2,5,1,1,'Internal Communication Workshop',DATE_ADD(@monday,INTERVAL 7 DAY),DATE_ADD(@monday,INTERVAL 7 DAY),0,0.5,b'1','UPDATED','Improve coordination across support teams.','Share workshop notes.',NULL,NULL),
(3,4,3,3,'Cloud Practitioner Certification',DATE_ADD(@monday,INTERVAL 14 DAY),DATE_ADD(@monday,INTERVAL 16 DAY),950,3,b'0','APPROVED','Develop cloud service knowledge.','Present a study checklist.','Aligned with the team development plan.',NULL),
(4,5,2,2,'Business Process Improvement',@past,DATE_ADD(@past,INTERVAL 2 DAY),620,3,b'0','COMPLETED','Improve daily administrative processes.','Document two process changes.','Approved for professional development.','Used the workshop methods to simplify the reporting process.'),
(5,4,2,2,'Advanced Reporting Techniques',DATE_ADD(@monday,INTERVAL 21 DAY),DATE_ADD(@monday,INTERVAL 23 DAY),720,3,b'0','REJECTED','Explore more advanced reporting methods.','Share report templates.','Please choose a course closer to this quarter priorities.',NULL),
(6,5,2,2,'Customer Service Practice',DATE_ADD(@monday,INTERVAL 28 DAY),DATE_ADD(@monday,INTERVAL 30 DAY),380,3,b'0','DELETED','Improve customer support communication.','Share service scripts.',NULL,NULL),
(7,3,2,2,'Coaching for Team Leads',DATE_ADD(@monday,INTERVAL 7 DAY),DATE_ADD(@monday,INTERVAL 9 DAY),800,3,b'0','APPLIED','Improve coaching of direct reports.','Use a structured coaching plan.',NULL,NULL);
INSERT INTO registration_requests (id,username,name,password_hash,status,submitted_at)
VALUES (1,'newstaff1','Demo New Colleague','{bcrypt}$2a$10$xDHvTayRuIzgAjsZdc1XZ.5hoW.lqOayDXeR6VvS6ONp6g3uGhDQq','PENDING',NOW());
COMMIT;
