# Email configuration

Course submission notifies the assigned manager. Approval or rejection notifies the employee and includes the decision reason.

## Runtime configuration

Email is disabled by default. Enable the `mail` profile and provide environment variables:

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Include `mail`; preserve other required profiles. |
| `CATS_MAIL_ENABLED` | `true` to enable notification creation and delivery. |
| `SMTP_HOST`, `SMTP_PORT` | Provider SMTP endpoint and port. |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | Sending account and provider-supported credential. |
| `SMTP_AUTH` | `true` for authenticated SMTP. |
| `SMTP_STARTTLS` | `true` when the provider requires STARTTLS. |
| `CATS_MAIL_FROM` | Authorized sender address. |
| `CATS_BASE_URL` | Application URL accessible to recipients. |

Defaults use a local SMTP capture server at `localhost:1025`, without authentication. This does not deliver to external inboxes. For Gmail testing, see [gmail-test.md](gmail-test.md).

Set recipient addresses in **Admin > Employee Management > Edit**. Employee addresses are recipients; they do not require SMTP credentials.

## Docker and EC2

Inject configuration and credentials when starting the container. Do not embed credentials in source code, the Dockerfile or image. Use a protected environment file or a secret-management service. Replace `localhost` in `CATS_BASE_URL` with the deployed application URL.

The current Dockerfile copies only the application JAR. Markdown documentation is also excluded by `.dockerignore`. This document, `gmail-test.md` and the local PowerShell launcher are not required in the runtime image.

## Delivery and monitoring

- Inspect **Admin > Email notifications** (`/admin/notifications`) for status, recipient snapshots, content and failures.
- Polling defaults to 10 seconds. Failed delivery retries after 60 and 120 seconds, with three attempts maximum.
- `SENT` means SMTP accepted the message; it does not guarantee inbox delivery.
- Missing recipient addresses create `FAILED` records without preventing the course action.
- Changing an employee address affects future notifications, not existing snapshots. No manual resend action is available.
- Enabling delivery may process existing eligible pending records. Disabling it preserves them.

## Persistence

With `spring.jpa.hibernate.ddl-auto=update`, startup creates `notification_outbox` and adds the nullable employee email field. Environments without automatic schema updates need an equivalent migration.

Application updates and outbox creation share a transaction. SMTP runs separately; delivery failure does not reverse submission or review. Delivery is at least once: an interruption after SMTP acceptance can cause a duplicate message.
