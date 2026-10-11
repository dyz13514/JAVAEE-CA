package com.group5.cats.service;

import com.group5.cats.dto.TrainingLedger;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.CourseFeeClaim;

public final class TrainingLedgerCsv {

    private TrainingLedgerCsv() {
    }

    public static String export(TrainingLedger ledger) {
        StringBuilder csv = new StringBuilder(CsvSupport.UTF8_BOM);
        csv.append(CsvSupport.row("Employee ID", "Employee name", "Course year"));
        csv.append(CsvSupport.row(ledger.getEmployee().getId().toString(),
                ledger.getEmployee().getName(), ledger.getYear().toString()));
        csv.append(CsvSupport.row("Entitlement configured", "Training budget", "Occupied budget",
                "Remaining budget", "Approved course fees", "Pending claims", "Approved unpaid claims", "Reimbursed claims"));

        String trainingBudget = "";
        String occupiedBudget = "";
        String remainingBudget = "";
        if (ledger.isHasEntitlement()) {
            trainingBudget = CsvSupport.formatMoney(ledger.getTrainingBudget());
            occupiedBudget = CsvSupport.formatMoney(ledger.getOccupiedBudget());
            remainingBudget = CsvSupport.formatMoney(ledger.getRemainingBudget());
        }
        csv.append(CsvSupport.row(Boolean.toString(ledger.isHasEntitlement()), trainingBudget,
                occupiedBudget, remainingBudget, ledger.getApprovedCourseFees().toPlainString(),
                ledger.getPendingClaimAmount().toPlainString(), ledger.getApprovedClaimAmount().toPlainString(),
                ledger.getReimbursedAmount().toPlainString()));
        csv.append(CsvSupport.row(""));
        csv.append(CsvSupport.row("Application ID", "Course", "Category", "Start date", "End date", "Course fee", "Status"));
        for (CourseApplication application : ledger.getApplications()) {
            csv.append(CsvSupport.row(application.getId().toString(), application.getCourseTitle(),
                    application.getCategoryName(), CsvSupport.formatDate(application.getFromDate()),
                    CsvSupport.formatDate(application.getToDate()), CsvSupport.formatMoney(application.getFee()),
                    application.getStatus()));
        }
        csv.append(CsvSupport.row(""));
        csv.append(CsvSupport.row("Claim ID", "Application ID", "Course", "Amount", "Status",
                "Submitted at", "Manager reason", "Payment date", "Payment reference"));
        for (CourseFeeClaim claim : ledger.getClaims()) {
            csv.append(CsvSupport.row(claim.getId().toString(), claim.getApplication().getId().toString(),
                    claim.getApplication().getCourseTitle(), claim.getAmount().toPlainString(),
                    claim.getStatus().name(), claim.getSubmittedAt().toString(), claim.getManagerReason(),
                    CsvSupport.formatDate(claim.getPaymentDate()), claim.getPaymentReference()));
        }
        return csv.toString();
    }
}
