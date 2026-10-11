package com.group5.cats.dto;

import java.math.BigDecimal;

public class CourseFeeClaimForm {

    private Long applicationId;

    private Long version;

    private BigDecimal amount;

    private boolean paidPersonally;

    public CourseFeeClaimForm() {
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public boolean isPaidPersonally() {
        return paidPersonally;
    }

    public void setPaidPersonally(boolean paidPersonally) {
        this.paidPersonally = paidPersonally;
    }
}
