-- CATS dummy data extracted from AWS RDS (cloud) on 2026-10-13
-- Data-only dump: run after the schema exists (JPA ddl-auto or demo-schema.sql).
-- Real rehearsal emails replaced with @example.com placeholders.
-- Host: 34.194.243.126    Database: cats
-- ------------------------------------------------------
-- Server version	8.4.9

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `annual_entitlements`
--

LOCK TABLES `annual_entitlements` WRITE;
/*!40000 ALTER TABLE `annual_entitlements` DISABLE KEYS */;
INSERT INTO `annual_entitlements` (`id`, `entitlement_year`, `training_budget`, `training_days_limit`, `employee_id`) VALUES (2,2026,20000,100,1);
INSERT INTO `annual_entitlements` (`id`, `entitlement_year`, `training_budget`, `training_days_limit`, `employee_id`) VALUES (3,2026,20000,50,2);
INSERT INTO `annual_entitlements` (`id`, `entitlement_year`, `training_budget`, `training_days_limit`, `employee_id`) VALUES (4,2026,20000,100,3);
INSERT INTO `annual_entitlements` (`id`, `entitlement_year`, `training_budget`, `training_days_limit`, `employee_id`) VALUES (5,2026,20000,50,4);
INSERT INTO `annual_entitlements` (`id`, `entitlement_year`, `training_budget`, `training_days_limit`, `employee_id`) VALUES (9,2026,20000,50,9);
/*!40000 ALTER TABLE `annual_entitlements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `claim_attachments`
--

LOCK TABLES `claim_attachments` WRITE;
/*!40000 ALTER TABLE `claim_attachments` DISABLE KEYS */;
INSERT INTO `claim_attachments` (`id`, `content_type`, `file_size`, `original_filename`, `stored_filename`, `uploaded_at`) VALUES (1,'image/png',252806,'微信图片_20261008160412_16_2.png','1ebe7c40-3863-43ef-8fdb-ed71fe88cb20.png','2026-10-13 10:16:43.823014');
INSERT INTO `claim_attachments` (`id`, `content_type`, `file_size`, `original_filename`, `stored_filename`, `uploaded_at`) VALUES (2,'image/png',252806,'微信图片_20261008160412_16_2.png','b090cbc3-eb13-4c45-b116-bfd081eb3cd1.png','2026-10-13 10:16:43.828413');
/*!40000 ALTER TABLE `claim_attachments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `claim_history`
--

LOCK TABLES `claim_history` WRITE;
/*!40000 ALTER TABLE `claim_history` DISABLE KEYS */;
INSERT INTO `claim_history` (`id`, `actor_id`, `actor_name`, `amount`, `comment`, `created_at`, `status`, `certificate_id`, `claim_id`, `receipt_id`) VALUES (1,3,'Ben Tan',200.00,'Claim submitted with receipt and certificate of completion.','2026-10-13 10:16:43.841565','PENDING',2,1,1);
/*!40000 ALTER TABLE `claim_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `common_courses`
--

LOCK TABLES `common_courses` WRITE;
/*!40000 ALTER TABLE `common_courses` DISABLE KEYS */;
INSERT INTO `common_courses` (`id`, `course_id`) VALUES (3,1);
INSERT INTO `common_courses` (`id`, `course_id`) VALUES (2,11);
INSERT INTO `common_courses` (`id`, `course_id`) VALUES (1,12);
/*!40000 ALTER TABLE `common_courses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `course_applications`
--

LOCK TABLES `course_applications` WRITE;
/*!40000 ALTER TABLE `course_applications` DISABLE KEYS */;
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (1,'INTERNAL','Cloud Architecture','',NULL,0,'2026-10-19',NULL,'Cloud email pipeline end-to-end test from cloud deployment','Approved for cloud migration preparation','CANCELLED','2026-10-21',3,1,10,3,1);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (2,'CERTIFICATION','Cybersecurity','231231',NULL,300,'2026-10-13',_binary '\0','321','noting','CANCELLED','2026-10-14',2,3,15,3,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (3,'CERTIFICATION','Cloud Architecture','312',NULL,300,'2026-10-13',_binary '\0','231','noting','CANCELLED','2026-10-14',2,3,12,3,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (4,'EXTERNAL','Cybersecurity','231',NULL,200,'2026-10-13',_binary '\0','321','21','CANCELLED','2026-10-14',2,2,14,3,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (5,'EXTERNAL','Cybersecurity','231',NULL,200,'2026-10-13',_binary '\0','321','w2','CANCELLED','2026-10-14',2,2,14,3,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (6,'EXTERNAL','Project Management','1',NULL,200,'2026-11-25',_binary '\0','1','dad','CANCELLED','2026-11-30',4,2,17,3,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (7,'CERTIFICATION','Cybersecurity','s',NULL,300,'2026-10-23',_binary '\0','s','da','CANCELLED','2026-10-30',6,3,15,3,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (8,'CERTIFICATION','Cloud Architecture','1',NULL,300,'2026-10-13',_binary '\0','1',NULL,'DELETED','2026-10-14',2,3,12,3,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (9,'CERTIFICATION','Cloud Architecture','1',NULL,300,'2026-10-13',_binary '\0','1',NULL,'DELETED','2026-10-14',2,3,12,3,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (10,'CERTIFICATION','Cloud Architecture','1',NULL,300,'2026-10-13',_binary '\0','1',NULL,'DELETED','2026-10-14',2,3,12,3,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (11,'EXTERNAL','Cloud Architecture','1',NULL,200,'2026-10-13',_binary '\0','1','1','APPROVED','2026-10-14',2,2,11,3,2);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (12,'CERTIFICATION','Java','1',NULL,300,'2026-10-15',_binary '\0','1',NULL,'APPLIED','2026-10-29',11,3,3,3,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (13,'CERTIFICATION','Cloud Architecture','1',NULL,300,'2026-10-14',_binary '\0','1',NULL,'APPLIED','2026-10-15',2,3,12,4,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (14,'EXTERNAL','Cloud Architecture','1',NULL,200,'2026-10-16',_binary '\0','1',NULL,'APPLIED','2026-10-19',2,2,11,4,2);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (15,'INTERNAL','Cloud Architecture','1',NULL,0,'2026-10-20',_binary '\0','1',NULL,'APPLIED','2026-10-21',2,1,10,4,1);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (16,'CERTIFICATION','Cybersecurity','1',NULL,300,'2026-10-22',_binary '\0','1',NULL,'APPLIED','2026-10-23',2,3,15,4,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (17,'EXTERNAL','Cybersecurity','1',NULL,200,'2026-10-26',_binary '\0','1',NULL,'APPLIED','2026-10-27',2,2,14,4,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (18,'INTERNAL','Cybersecurity','1',NULL,0,'2026-10-28',_binary '\0','1',NULL,'APPLIED','2026-10-29',2,1,13,4,1);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (19,'CERTIFICATION','Java','1',NULL,300,'2026-11-02',_binary '\0','1',NULL,'APPLIED','2026-11-03',2,3,3,4,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (20,'EXTERNAL','Java','1',NULL,200,'2026-11-05',_binary '\0','1',NULL,'APPLIED','2026-11-06',2,2,2,4,2);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (21,'EXTERNAL','Java','1',NULL,200,'2026-10-08',_binary '\0','1',NULL,'COMPLETED','2026-10-09',2,2,2,3,2);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (22,'EXTERNAL','Project Management','1',NULL,200,'2026-12-22',_binary '\0','1',NULL,'APPLIED','2026-12-23',2,2,17,4,3);
INSERT INTO `course_applications` (`id`, `category_name_snapshot`, `course_title`, `dissemination`, `experience_comments`, `fee`, `from_date`, `half_day`, `justification`, `manager_comment`, `status`, `to_date`, `training_days`, `category_id`, `course_id`, `employee_id`, `provider_id`) VALUES (23,'CERTIFICATION','Cloud Architecture','1',NULL,300,'2026-11-09',_binary '\0','1','123','APPROVED','2026-11-10',2,3,12,3,3);
/*!40000 ALTER TABLE `course_applications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `course_categories`
--

LOCK TABLES `course_categories` WRITE;
/*!40000 ALTER TABLE `course_categories` DISABLE KEYS */;
INSERT INTO `course_categories` (`id`, `description`, `half_day_allowed`, `name`) VALUES (1,'Internal training provided by the organisation.',_binary '','INTERNAL');
INSERT INTO `course_categories` (`id`, `description`, `half_day_allowed`, `name`) VALUES (2,'External courses provided by training providers.',_binary '\0','EXTERNAL');
INSERT INTO `course_categories` (`id`, `description`, `half_day_allowed`, `name`) VALUES (3,'Training for professional certification.',_binary '\0','CERTIFICATION');
/*!40000 ALTER TABLE `course_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `course_fee_claims`
--

LOCK TABLES `course_fee_claims` WRITE;
/*!40000 ALTER TABLE `course_fee_claims` DISABLE KEYS */;
INSERT INTO `course_fee_claims` (`id`, `amount`, `manager_reason`, `paid_at`, `paid_by_id`, `paid_by_name`, `paid_personally`, `payment_date`, `payment_note`, `payment_reference`, `reviewed_at`, `reviewed_by_id`, `reviewed_by_name`, `status`, `submitted_at`, `version`, `application_id`, `certificate_id`, `receipt_id`) VALUES (1,200.00,NULL,NULL,NULL,NULL,_binary '',NULL,NULL,NULL,NULL,NULL,NULL,'PENDING','2026-10-13 10:16:43.829746',0,21,2,1);
/*!40000 ALTER TABLE `course_fee_claims` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `courses`
--

LOCK TABLES `courses` WRITE;
/*!40000 ALTER TABLE `courses` DISABLE KEYS */;
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (1,_binary '',0,'A practical introduction to Java. Learn through guided exercises.','Java',1,1);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (2,_binary '',200,'A practical introduction to Java. Learn through guided exercises.','Java',2,2);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (3,_binary '',300,'A practical introduction to Java. Learn through guided exercises.','Java',3,3);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (4,_binary '',0,'A practical introduction to Python. Learn through guided exercises.','Python',1,1);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (5,_binary '',200,'A practical introduction to Python. Learn through guided exercises.','Python',2,3);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (6,_binary '',300,'A practical introduction to Python. Learn through guided exercises.','Python',3,3);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (7,_binary '',0,'A practical introduction to SQL. Learn through guided exercises.','SQL',1,1);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (8,_binary '',200,'A practical introduction to SQL. Learn through guided exercises.','SQL',2,3);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (9,_binary '',300,'A practical introduction to SQL. Learn through guided exercises.','SQL',3,3);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (10,_binary '',0,'A practical introduction to Cloud Architecture. Learn through guided exercises.','Cloud Architecture',1,1);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (11,_binary '',200,'A practical introduction to Cloud Architecture. Learn through guided exercises.','Cloud Architecture',2,2);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (12,_binary '',300,'A practical introduction to Cloud Architecture. Learn through guided exercises.','Cloud Architecture',3,3);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (13,_binary '',0,'A practical introduction to Cybersecurity. Learn through guided exercises.','Cybersecurity',1,1);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (14,_binary '',200,'A practical introduction to Cybersecurity. Learn through guided exercises.','Cybersecurity',2,3);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (15,_binary '',300,'A practical introduction to Cybersecurity. Learn through guided exercises.','Cybersecurity',3,3);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (16,_binary '',0,'A practical introduction to Project Management. Learn through guided exercises.','Project Management',1,1);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (17,_binary '',200,'A practical introduction to Project Management. Learn through guided exercises.','Project Management',2,3);
INSERT INTO `courses` (`id`, `active`, `fee`, `introduction`, `title`, `category_id`, `provider_id`) VALUES (18,_binary '',300,'A practical introduction to Project Management. Learn through guided exercises.','Project Management',3,3);
/*!40000 ALTER TABLE `courses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `employees`
--

LOCK TABLES `employees` WRITE;
/*!40000 ALTER TABLE `employees` DISABLE KEYS */;
INSERT INTO `employees` (`id`, `designation`, `email`, `name`, `password`, `role`, `username`, `supervisor_id`) VALUES (1,'PROFESSIONAL','manager1@example.com','Alice Wong','manager123','MANAGER','manager1',NULL);
INSERT INTO `employees` (`id`, `designation`, `email`, `name`, `password`, `role`, `username`, `supervisor_id`) VALUES (2,'ADMINISTRATIVE',NULL,'System Admin','admin123','ADMIN','admin',NULL);
INSERT INTO `employees` (`id`, `designation`, `email`, `name`, `password`, `role`, `username`, `supervisor_id`) VALUES (3,'PROFESSIONAL','emp1@example.com','Ben Tan','emp123','REGULAR_STAFF','emp1',1);
INSERT INTO `employees` (`id`, `designation`, `email`, `name`, `password`, `role`, `username`, `supervisor_id`) VALUES (4,'ADMINISTRATIVE','emp2@example.com','Cathy Lim','emp123','REGULAR_STAFF','emp2',1);
INSERT INTO `employees` (`id`, `designation`, `email`, `name`, `password`, `role`, `username`, `supervisor_id`) VALUES (9,'ADMINISTRATIVE',NULL,'Syr','{bcrypt}$2a$10$X794R.clRa4wrycxcqlEyOhVjypntjKwhTp3Pt9kMVu5qnZrHiFvy','REGULAR_STAFF','emp3',1);
/*!40000 ALTER TABLE `employees` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `notification_outbox`
--

LOCK TABLES `notification_outbox` WRITE;
/*!40000 ALTER TABLE `notification_outbox` DISABLE KEYS */;
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (1,'Applicant: Ben Tan\nCourse: Cloud Architecture\nPeriod: 2026-10-19 to 2026-10-21\nApplication: #1\nStatus: APPLIED\n\nLog in to view the application and comments: http://localhost:8080/employee/login','2026-10-12 13:50:54.659557','1:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-12 13:50:57.353507','CATS: APPLICATION SUBMITTED #1',1,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (2,'Applicant: Ben Tan\nCourse: Cybersecurity\nPeriod: 2026-10-13 to 2026-10-14\nApplication: #2\nStatus: APPLIED\n\nLog in to view the application and comments: http://localhost:8080/employee/login','2026-10-12 14:00:43.454436','2:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-12 14:00:48.854875','CATS: APPLICATION SUBMITTED #2',2,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (3,'Applicant: Ben Tan\nCourse: Cybersecurity\nPeriod: 2026-10-13 to 2026-10-14\nApplication: #2\nStatus: APPROVED\nManager reason: noting\n\nLog in to view the application and comments: http://localhost:8080/employee/login','2026-10-12 14:02:06.657110','2:APPLICATION_APPROVED','SENT',NULL,NULL,'APPLICATION_APPROVED','emp1@example.com',1,'2026-10-12 14:02:10.123985','CATS: APPLICATION APPROVED #2',2,3);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (4,'Applicant: Ben Tan\nCourse: Cloud Architecture\nPeriod: 2026-10-13 to 2026-10-14\nApplication: #3\nStatus: APPLIED\n\nLog in to view the application and comments: http://localhost:8080/employee/login','2026-10-12 14:03:27.702642','3:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-12 14:03:31.307893','CATS: APPLICATION SUBMITTED #3',3,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (5,'Applicant: Ben Tan\nCourse: Cloud Architecture\nPeriod: 2026-10-13 to 2026-10-14\nApplication: #3\nStatus: APPROVED\nManager reason: noting\n\nLog in to view the application and comments: http://localhost:8080/employee/login','2026-10-12 14:04:16.475875','3:APPLICATION_APPROVED','SENT',NULL,NULL,'APPLICATION_APPROVED','emp1@example.com',1,'2026-10-12 14:04:22.458924','CATS: APPLICATION APPROVED #3',3,3);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (6,'Applicant: Ben Tan\nCourse: Cloud Architecture\nPeriod: 2026-10-19 to 2026-10-21\nApplication: #1\nStatus: APPROVED\nManager reason: Approved for cloud migration preparation\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-12 14:32:41.384608','1:APPLICATION_APPROVED','SENT',NULL,NULL,'APPLICATION_APPROVED','emp1@example.com',1,'2026-10-12 14:32:49.690737','CATS: APPLICATION APPROVED #1',1,3);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (7,'Applicant: Ben Tan\nCourse: Cybersecurity\nPeriod: 2026-10-13 to 2026-10-14\nApplication: #4\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-12 14:37:31.879425','4:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-12 14:37:41.169280','CATS: APPLICATION SUBMITTED #4',4,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (8,'Applicant: Ben Tan\nCourse: Cybersecurity\nPeriod: 2026-10-13 to 2026-10-14\nApplication: #4\nStatus: APPROVED\nManager reason: 21\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-12 14:38:21.796606','4:APPLICATION_APPROVED','SENT',NULL,NULL,'APPLICATION_APPROVED','emp1@example.com',1,'2026-10-12 14:38:32.405506','CATS: APPLICATION APPROVED #4',4,3);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (9,'Applicant: Ben Tan\nCourse: Cybersecurity\nPeriod: 2026-10-13 to 2026-10-14\nApplication: #5\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-12 14:51:21.564961','5:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-12 14:51:23.857315','CATS: APPLICATION SUBMITTED #5',5,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (10,'Applicant: Ben Tan\nCourse: Cybersecurity\nPeriod: 2026-10-13 to 2026-10-14\nApplication: #5\nStatus: APPROVED\nManager reason: w2\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-12 14:51:57.684864','5:APPLICATION_APPROVED','SENT',NULL,NULL,'APPLICATION_APPROVED','emp1@example.com',1,'2026-10-12 14:52:04.943288','CATS: APPLICATION APPROVED #5',5,3);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (11,'Applicant: Ben Tan\nCourse: Project Management\nPeriod: 2026-11-25 to 2026-11-30\nApplication: #6\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-12 20:09:14.612961','6:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-12 20:09:21.793940','CATS: APPLICATION SUBMITTED #6',6,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (12,'Applicant: Ben Tan\nCourse: Project Management\nPeriod: 2026-11-25 to 2026-11-30\nApplication: #6\nStatus: APPROVED\nManager reason: dad\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-12 20:10:28.172130','6:APPLICATION_APPROVED','SENT',NULL,NULL,'APPLICATION_APPROVED','emp1@example.com',1,'2026-10-12 20:10:32.901121','CATS: APPLICATION APPROVED #6',6,3);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (13,'Applicant: Ben Tan\nCourse: Cybersecurity\nPeriod: 2026-10-23 to 2026-10-30\nApplication: #7\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-12 21:14:08.087964','7:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-12 21:14:15.176759','CATS: APPLICATION SUBMITTED #7',7,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (14,'Applicant: Ben Tan\nCourse: Cybersecurity\nPeriod: 2026-10-23 to 2026-10-30\nApplication: #7\nStatus: APPROVED\nManager reason: da\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-12 21:15:27.618102','7:APPLICATION_APPROVED','SENT',NULL,NULL,'APPLICATION_APPROVED','emp1@example.com',1,'2026-10-12 21:15:36.210779','CATS: APPLICATION APPROVED #7',7,3);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (15,'Applicant: Ben Tan\nCourse: Cloud Architecture\nPeriod: 2026-10-13 to 2026-10-14\nApplication: #8\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-12 23:34:04.090779','8:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-12 23:34:09.397865','CATS: APPLICATION SUBMITTED #8',8,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (16,'Applicant: Ben Tan\nCourse: Cloud Architecture\nPeriod: 2026-10-13 to 2026-10-14\nApplication: #9\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-12 23:34:46.229067','9:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',2,'2026-10-12 23:36:03.257236','CATS: APPLICATION SUBMITTED #9',9,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (17,'Applicant: Ben Tan\nCourse: Cloud Architecture\nPeriod: 2026-10-13 to 2026-10-14\nApplication: #10\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-12 23:35:39.181629','10:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-12 23:35:42.155760','CATS: APPLICATION SUBMITTED #10',10,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (18,'Applicant: Ben Tan\nCourse: Cloud Architecture\nPeriod: 2026-10-13 to 2026-10-14\nApplication: #11\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-12 23:37:14.828359','11:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-12 23:37:24.651640','CATS: APPLICATION SUBMITTED #11',11,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (19,'Applicant: Ben Tan\nCourse: Java\nPeriod: 2026-10-15 to 2026-10-29\nApplication: #12\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-12 23:40:04.377448','12:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-12 23:40:05.826640','CATS: APPLICATION SUBMITTED #12',12,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (20,'Applicant: Ben Tan\nCourse: Cloud Architecture\nPeriod: 2026-10-13 to 2026-10-14\nApplication: #11\nStatus: APPROVED\nManager reason: 1\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-13 01:46:49.644370','11:APPLICATION_APPROVED','SENT',NULL,NULL,'APPLICATION_APPROVED','emp1@example.com',1,'2026-10-13 01:47:00.479253','CATS: APPLICATION APPROVED #11',11,3);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (21,'Applicant: Cathy Lim\nCourse: Cloud Architecture\nPeriod: 2026-10-14 to 2026-10-15\nApplication: #13\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-13 09:39:43.464053','13:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-13 09:39:49.731224','CATS: APPLICATION SUBMITTED #13',13,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (22,'Applicant: Cathy Lim\nCourse: Cloud Architecture\nPeriod: 2026-10-16 to 2026-10-19\nApplication: #14\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-13 09:40:52.765480','14:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-13 09:41:03.999544','CATS: APPLICATION SUBMITTED #14',14,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (23,'Applicant: Cathy Lim\nCourse: Cloud Architecture\nPeriod: 2026-10-20 to 2026-10-21\nApplication: #15\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-13 09:41:17.002499','15:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-13 09:41:25.240668','CATS: APPLICATION SUBMITTED #15',15,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (24,'Applicant: Cathy Lim\nCourse: Cybersecurity\nPeriod: 2026-10-22 to 2026-10-23\nApplication: #16\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-13 09:41:44.245781','16:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-13 09:41:47.149910','CATS: APPLICATION SUBMITTED #16',16,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (25,'Applicant: Cathy Lim\nCourse: Cybersecurity\nPeriod: 2026-10-26 to 2026-10-27\nApplication: #17\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-13 09:42:11.812688','17:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-13 09:42:19.372618','CATS: APPLICATION SUBMITTED #17',17,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (26,'Applicant: Cathy Lim\nCourse: Cybersecurity\nPeriod: 2026-10-28 to 2026-10-29\nApplication: #18\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-13 09:42:29.674286','18:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-13 09:42:40.607420','CATS: APPLICATION SUBMITTED #18',18,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (27,'Applicant: Cathy Lim\nCourse: Java\nPeriod: 2026-11-02 to 2026-11-03\nApplication: #19\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-13 09:43:19.329736','19:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-13 09:43:21.665658','CATS: APPLICATION SUBMITTED #19',19,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (28,'Applicant: Cathy Lim\nCourse: Java\nPeriod: 2026-11-05 to 2026-11-06\nApplication: #20\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-13 09:43:48.006098','20:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-13 09:43:52.924447','CATS: APPLICATION SUBMITTED #20',20,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (29,'Applicant: Cathy Lim\nCourse: Project Management\nPeriod: 2026-12-22 to 2026-12-23\nApplication: #22\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-13 10:36:33.047608','22:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-13 10:36:36.976368','CATS: APPLICATION SUBMITTED #22',22,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (30,'Applicant: Ben Tan\nCourse: Cloud Architecture\nPeriod: 2026-11-09 to 2026-11-10\nApplication: #23\nStatus: APPLIED\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-13 11:12:27.185337','23:APPLICATION_SUBMITTED','SENT',NULL,NULL,'APPLICATION_SUBMITTED','manager1@example.com',1,'2026-10-13 11:12:29.241000','CATS: APPLICATION SUBMITTED #23',23,1);
INSERT INTO `notification_outbox` (`id`, `body`, `created_at`, `deduplication_key`, `delivery_status`, `last_error`, `next_attempt_at`, `notification_type`, `recipient_email_snapshot`, `retry_count`, `sent_at`, `subject`, `application_id`, `recipient_employee_id`) VALUES (31,'Applicant: Ben Tan\nCourse: Cloud Architecture\nPeriod: 2026-11-09 to 2026-11-10\nApplication: #23\nStatus: APPROVED\nManager reason: 123\n\nLog in to view the application and comments: http://34.194.243.126:8080/employee/login','2026-10-13 11:13:59.094939','23:APPLICATION_APPROVED','SENT',NULL,NULL,'APPLICATION_APPROVED','emp1@example.com',1,'2026-10-13 11:14:00.422692','CATS: APPLICATION APPROVED #23',23,3);
/*!40000 ALTER TABLE `notification_outbox` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `public_holidays`
--

LOCK TABLES `public_holidays` WRITE;
/*!40000 ALTER TABLE `public_holidays` DISABLE KEYS */;
INSERT INTO `public_holidays` (`id`, `holiday_date`, `name`) VALUES (1,'2026-10-30','break');
INSERT INTO `public_holidays` (`id`, `holiday_date`, `name`) VALUES (3,'2026-11-05','nus well day');
/*!40000 ALTER TABLE `public_holidays` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `registration_requests`
--

LOCK TABLES `registration_requests` WRITE;
/*!40000 ALTER TABLE `registration_requests` DISABLE KEYS */;
INSERT INTO `registration_requests` (`id`, `name`, `password_hash`, `review_reason`, `reviewed_at`, `status`, `submitted_at`, `username`, `reviewed_by_id`) VALUES (1,'ssr',NULL,NULL,'2026-10-12 23:08:17.127800','APPROVED','2026-10-12 23:06:59.419648','ssyr',2);
INSERT INTO `registration_requests` (`id`, `name`, `password_hash`, `review_reason`, `reviewed_at`, `status`, `submitted_at`, `username`, `reviewed_by_id`) VALUES (2,'Bob',NULL,NULL,'2026-10-13 11:07:26.480971','APPROVED','2026-10-13 11:06:28.501414','manager2',2);
INSERT INTO `registration_requests` (`id`, `name`, `password_hash`, `review_reason`, `reviewed_at`, `status`, `submitted_at`, `username`, `reviewed_by_id`) VALUES (3,'Syr',NULL,NULL,'2026-10-13 11:52:22.108029','APPROVED','2026-10-13 11:51:38.932540','emp3',2);
/*!40000 ALTER TABLE `registration_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `training_providers`
--

LOCK TABLES `training_providers` WRITE;
/*!40000 ALTER TABLE `training_providers` DISABLE KEYS */;
INSERT INTO `training_providers` (`id`, `name`) VALUES (2,'AWS');
INSERT INTO `training_providers` (`id`, `name`) VALUES (3,'Demo Skills Academy');
INSERT INTO `training_providers` (`id`, `name`) VALUES (1,'ISS');
/*!40000 ALTER TABLE `training_providers` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-13 12:08:40
