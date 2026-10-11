# Course fee claim management

This module implements the optional Course Fee Claim Management requirement in
SA63 CA Instructions. It follows the existing Controller / Service interface /
Service implementation / Repository / Entity structure. Java classes use explicit
fields, constructors, getters, setters, conditions and loops, consistent with the
Spring MVC, service transaction and JPA examples in the Suria teaching materials.

## Workflow

1. An employee completes an approved EXTERNAL or CERTIFICATION course and opens
   **My claims**, or **Course fee claim** on the completed application detail.
2. The employee selects the course, enters a positive amount no larger than the
   course fee, confirms payment from personal funds, and attaches both a receipt
   and a certificate of completion. The employee must have an assigned manager.
3. The claim becomes PENDING. Only the employee's current assigned manager can
   approve or reject it. A reason is required for either decision. Managers cannot
   review their own claims.
4. REJECTED claims may be revised and resubmitted by their owner. The course is
   fixed; the amount and attachments may be revised. Previous amounts, reasons
   and attachments remain in the history.
5. An administrator opens **Reimbursements** and records an actual payment for
   an APPROVED claim, including payment date and reference. This changes the claim
   to REIMBURSED. The system records payments made outside the application; it does
   not transfer money.

Only one claim exists per course application. A database unique constraint
enforces this rule. An entity version prevents competing reviews, resubmissions
or payment recordings from overwriting each other. Claim changes and history
entries are saved in the same transaction.

## Training ledger

**Training ledger** shows the existing annual training budget and occupied /
remaining budget, approved course fees, pending claims, approved unpaid claims,
and actual reimbursed claims. Rejected claims remain visible without contributing
to those totals. Employees see their own records; managers may also see their
current direct reports; administrators may see all employees. CSV export uses
the same permissions and the existing CSV escaping helpers.

Course fees count in the year of the application's start date, following the
existing entitlement calculation. Claims use that same course year even if their
payment occurs later; payment dates are displayed separately. Claim approval or
payment does not alter the course status and does not deduct its budget again.
Existing manager reports retain their current calculations and rendering; actual
reimbursement totals are available on the new ledger.

## Attachments and configuration

Accepted formats are PDF, PNG and JPEG, up to 10 MB for each file. Both filename
extension and content signature are checked. Stored filenames are generated UUIDs.
The original filename is retained for download only. Files are private and are
served through a claim-specific download endpoint, which authorises the owner,
the current assigned manager or an administrator. Earlier attachments remain
available through the authorised history. Files written for a failed transaction
are removed on rollback; successful historical files are retained.

The default storage location is `./uploads/claims`, relative to the application
working directory. Override it with the `CATS_CLAIM_UPLOAD_DIRECTORY` environment
variable, or the `cats.claims.upload-directory` property. The directory must be
writable and must not be configured as a public static resource directory.
Uploads are excluded from Git and Docker build contexts. Docker Compose uses a
separate persistent `cats-claim-files` volume. Back up attachment storage together
with the database; restoring only one will leave inconsistent records.

The multipart request limits are 10 MB per file and 24 MB per request. The latter
allows both required files and the remaining form fields. Multipart parsing is
lazy so the claim controller can show a useful message when an upload is too large.

## Database and existing code

Three new tables are introduced: `course_fee_claims`, `claim_attachments` and
`claim_history`. Existing application, employee, category and entitlement entities
are referenced without adding claim fields or changing their existing mappings.
The existing `spring.jpa.hibernate.ddl-auto=update` setting creates the new tables
at the next normal application startup. No old course migration is included in
this change. Deployments that disable automatic DDL must provision the equivalent
new tables and constraints before enabling the module.

Existing CSS, JavaScript, layout structure and page rendering are reused. The only
additions to existing templates are navigation links and a claim link for completed
eligible courses. The claim and ledger pages are new templates.

## Verification for this change

At the user's request, automated tests and application / database startup were
not run during implementation. Offline Maven compilation (`mvnw -o
-Dmaven.test.skip=true compile`) succeeded. Source review covered the permitted
status transitions, ownership and manager checks, version and uniqueness guards,
rollback file cleanup, ledger calculations and the limited existing-template
changes. Runtime behaviour still needs validation against the user's restored
database.
