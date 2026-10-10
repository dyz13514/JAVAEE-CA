# Local Gmail delivery test

This procedure creates application records and sends real email. Use test accounts and inboxes you control.

## Preparation

1. Enable Google two-step verification and generate an [app password](https://support.google.com/mail/answer/185833). Some managed accounts restrict this feature.
2. In **Admin > Employee Management**, configure employee and manager email addresses and assign the employee to that manager. Both addresses may use the same test inbox.
3. Configure annual entitlement for the course start year, with sufficient remaining training days. Check existing pending email notifications before enabling delivery.
4. Stop any CATS process using port 8080.

## Start

From the project root, in CMD or PowerShell:

```text
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\start-gmail-test.ps1
```

Enter the sending Gmail address and app password at the prompts. Password input is hidden. The script configures `smtp.gmail.com:587`, authentication, STARTTLS and the `mail` profile. Credentials are held in process environment variables, not written to files. Stop with Ctrl+C.

This launcher is for local testing only. Docker/EC2 deployments supply environment variables directly; see [email-setup.md](email-setup.md).

## Submit an application

Log in as the employee and select **Apply for course**:

| Field | Test value |
| --- | --- |
| Course Title | `Email Delivery Test - Approval` |
| Category | `INTERNAL` |
| Training Provider | `CATS Internal Training` |
| From / To Date | Two future working days, excluding holidays and existing application conflicts. |
| Half-day | Unchecked |
| Actual Course Fee | `0` |
| Reason for Training | `Verify course application email delivery.` |
| Knowledge Sharing Plan | `Share test results with the project team.` |

Current full-day validation requires the end date to be later than the start date. INTERNAL courses still require annual training-day entitlement.

## Verify

1. Submit and note the application ID. Confirm the manager receives the submission message.
2. As the assigned manager, open **Team applications** and approve with reason `Approved for email delivery testing.` Confirm the employee receives the decision and reason.
3. Submit another application titled `Email Delivery Test - Rejection`, using different non-overlapping dates. Reject it with reason `Rejected intentionally for email delivery testing.`
4. In **Admin > Email notifications**, match records by application ID. Expect four notifications: two submissions, one approval and one rejection. Delivery normally starts within a polling cycle of approximately 10 seconds.

Existing applications do not generate retrospective notifications simply because email is enabled.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| No notification record | Mail enabled and the application action succeeded. |
| Missing recipient | Employee/manager address was configured before the action. |
| Authentication failed | Complete Gmail address and valid app password. |
| Connection timeout | Network access to `smtp.gmail.com:587`. |
| SENT but no message | Recipient snapshot and spam folder. |

Do not commit credentials. This document and the launcher are not required in the Docker runtime image.
