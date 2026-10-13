# CATS — Course Application Tracking System

Group 5 · NUS-ISS SA63 Continuous Assessment

CATS is an internal training management system. Employees apply for courses, managers
review and approve applications, and administrators maintain the reference data and
process course fee claims. The application was developed with Spring Boot (server-rendered
Thymeleaf pages, no separate front-end build) and deployed to AWS as a Docker container.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.1.1 (Web MVC, Thymeleaf, Data JPA, Validation, Mail) |
| Security | Spring Security Crypto (BCrypt password hashing), role-based portals |
| Database | MySQL 8 (schema auto-created by JPA `ddl-auto=update`) |
| Build | Maven (`pom.xml` included) |
| Deployment | Docker image → Amazon ECR → AWS ECS Fargate behind a Network Load Balancer, with Amazon RDS MySQL 8.4 |

## How to Run

**Prerequisites:** JDK 17+, Maven 3.8+, MySQL 8.x running on localhost.

1. Create the database (once):

   ```sql
   CREATE DATABASE cats;
   ```

2. Point the app at your MySQL. Either set the environment variables
   `DB_USERNAME` / `DB_PASSWORD`, or edit `src/main/resources/application.properties`
   (defaults: `jdbc:mysql://localhost:3306/cats`, user `root`).

3. Start the app:

   ```bash
   mvn spring-boot:run
   ```

   On first boot all tables are created automatically, and the built-in data loaders
   seed the base reference data (categories, courses, entitlements) and the four core
   accounts below — the app is usable immediately.

4. (Recommended) Populate the full dummy dataset — 23 course applications with
   approvals, email notifications, claims and registration requests — by executing
   `database/cats-data-cloud.sql` against the `cats` schema after the first boot:

   ```bash
   mysql -u root -p cats < database/cats-data-cloud.sql
   ```

   The script is a plain data dump (INSERT statements only, one row per statement),
   idempotent to re-run after the tables are cleared.

5. Open <http://localhost:8080>.

Email notifications are optional; SMTP settings live in `application-mail.properties`
and mail delivery is disabled by default.

## Test Accounts

| Username | Password | Role | Notes |
|---|---|---|---|
| `admin` | `admin123` | Administrator | Maintains employees, courses, holidays; processes claims |
| `manager1` | `manager123` | Manager (Alice Wong) | Supervisor of emp1 / emp2; approval inbox and reports |
| `emp1` | `emp123` | Employee (Ben Tan) | Main applicant account |
| `emp2` | `emp123` | Employee (Cathy Lim) | Second team member |

(The dummy dataset also contains pending/approved self-registration requests for the
registration-approval workflow demo, and one account created through the registration UI.)

## Features

**Core**

- Dual-portal login (Employee/Manager vs Administrator) with BCrypt-hashed credentials
- Course application: course / category / provider selection, fee inherited from the course
- Training-day computation: weekends and admin-maintained public holidays excluded,
  optional half-day last training day for allowed categories
- Annual training entitlement check per employee and year
- Manager approval workflow with review comments; application status lifecycle
  (APPLIED / UPDATED / APPROVED / REJECTED / COMPLETED / CANCELLED)
- Course fee claims: submitted after course completion, with receipt attachments,
  approval and payment processing by the administrator
- Administrator maintenance: employees, courses, categories, providers, public holidays

**Beyond the basics**

- Self-service registration with administrator approval workflow (`registration_requests`)
- Transactional email notifications implemented as an **outbox pattern**: messages are
  persisted in `notification_outbox` and dispatched by a background poller with retry,
  backoff and deduplication keys — no email is lost if SMTP is temporarily down
- Claim audit trail: every claim state change is versioned in `claim_history`
- Data-integrity guards in the admin UI: the last administrator cannot be deleted or
  demoted, a manager with subordinates cannot be deleted, circular supervisor
  relationships are rejected
- Commonly-attended courses catalogue to speed up applications
- Manager attendance report over a selectable date range
- Search / filter / pagination on the application lists
- Claim attachments stored on disk with size limits and lazy multipart resolution

**Cloud deployment (demonstrated in the video)**

The same Docker image was pushed to Amazon ECR and run as an AWS ECS **Fargate** task
(serverless containers) behind a Network Load Balancer with a fixed Elastic IP, using
Amazon RDS MySQL 8.4 in the same VPC. Database credentials and SMTP settings are
injected as environment variables at task start — no secrets inside the image.
(The cloud environment is temporary and may be offline outside the demonstration period.)

## Project Structure

```
src/main/java/com/group5/cats/   controllers, services, repositories, entities, data loaders
src/main/resources/templates/    Thymeleaf views (employee / manager / admin portals)
src/main/resources/static/       CSS, icons, client-side JS
database/                        dummy-data SQL script (cats-data-cloud.sql)
```
