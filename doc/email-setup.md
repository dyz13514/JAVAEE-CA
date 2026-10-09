# Email notifications

The optional email feature sends a notification to the supervisor when a course application is submitted, and to the employee when it is approved or rejected. Decisions include the manager's reason and a link to the employee login entry point.

## Enable locally

Email is disabled by default. Existing accounts and login flows continue to work without SMTP settings. Activate the `mail` Spring profile to enable delivery:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'mail'
$env:SMTP_HOST = 'localhost'
$env:SMTP_PORT = '1025'
$env:CATS_MAIL_FROM = 'cats@example.test'
$env:CATS_BASE_URL = 'http://localhost:8080'
.\mvnw.cmd spring-boot:run
```

Start a local SMTP capture server listening on port 1025 before this command. A local capture server is sufficient for the CA demonstration; it does not deliver to real inboxes. In the IDE, set the same environment variables in the application's Run Configuration. If other profiles are needed, add `mail` to the comma-separated profile list.

For real SMTP, set `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH=true` and, when the provider requires STARTTLS, `SMTP_STARTTLS=true`. Use the provider's correct settings. Never commit passwords or authorization codes. `CATS_BASE_URL` must be an address the email recipient can access; localhost works only on the same computer.

Configure employee and manager email addresses in Admin > Employee Management > Edit. Existing accounts allow a blank address to preserve compatibility; a missing address produces a FAILED outbox record instead of preventing the course action. No real addresses are seeded automatically.

## Database changes

The existing `spring.jpa.hibernate.ddl-auto=update` creates `notification_outbox` and adds the nullable `employees.email` column when the application next starts. This happens even with delivery disabled because the new entity is mapped. Back up the database before applying schema changes. No existing table or data is dropped. Environments that disable automatic schema changes must apply equivalent additive DDL through their usual migration process.

The outbox stores recipient and content snapshots, three notification types, delivery status, attempt count, retry time, generic failure reason, unique event key and timestamps. The optional claim relationship from the ER diagram is deferred until a claim entity exists.

Application changes and outbox insertion share a database transaction. SMTP runs later and outside that transaction. SMTP failure does not undo approval or submission; database failure to insert the outbox rolls back the business transaction. The service preserves existing validation and return messages.

## Verify delivery

Administrators can open **Email notifications** in the sidebar or visit `/admin/notifications`. The read-only page supports status filtering, 20-record pagination and expandable subject, body, sent time, next attempt/lease expiry and failure reason. Non-admin users cannot access it. The page remains available when delivery is disabled. No retry or send action is exposed.

1. Configure emails for an employee and their supervisor.
2. Submit a valid application. Confirm the page succeeds and a PENDING outbox record appears.
3. Within the polling interval (default 10 seconds), check the local capture inbox and SENT status.
4. Approve another application and reject another; confirm the applicant receives the matching decision and reason.
5. Stop SMTP and submit a new application. The application still succeeds. Delivery retries after 60 seconds and then 120 seconds; it stops after three failed attempts.

```sql
SELECT id, notification_type, recipient_email_snapshot, delivery_status,
       retry_count, next_attempt_at, last_error, sent_at
FROM notification_outbox ORDER BY id DESC;
```

To disable creation and sending, remove the mail profile or set `CATS_MAIL_ENABLED=false` while using it. Existing pending records remain stored for later delivery.

SENT means the SMTP server accepted the message, not that it was read or reached an inbox. A unique key prevents duplicate creation for the same application event. Conditional updates and a five-minute lease prevent concurrent workers from sending the same active record. Expired leases are reclaimed after a crash. Delivery is at least once: a crash after SMTP acceptance but before the SENT update can cause a duplicate email. Keep configured SMTP timeouts shorter than the lease.

Existing failed records are not automatically resent when an employee's email changes: their address is a snapshot. Correct the account for future events. No public resend endpoint is provided in this version. Employees referenced by notification history cannot be deleted.

## Isolated regression tests

```powershell
.\mvnw.cmd '-Dspring.datasource.url=jdbc:h2:mem:regression;DB_CLOSE_DELAY=-1' '-Dspring.datasource.driver-class-name=org.h2.Driver' '-Dspring.datasource.username=sa' '-Dspring.datasource.password=' '-Dspring.jpa.hibernate.ddl-auto=create-drop' test
```

Tests cover content, recipients, disabled mode, missing addresses, deduplication, rollback, leasing, retries, employee email validation and SMTP communication with a loopback-only test server. H2 is a test dependency; these tests do not use the application's MySQL database or send external email.
