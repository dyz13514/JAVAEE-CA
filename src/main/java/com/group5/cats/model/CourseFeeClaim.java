package com.group5.cats.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "course_fee_claims",
       uniqueConstraints = @UniqueConstraint(name = "uk_claim_application", columnNames = "application_id"))
public class CourseFeeClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @OneToOne(optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private CourseApplication application;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimStatus status;

    @Column(nullable = false)
    private boolean paidPersonally;

    @ManyToOne(optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    private ClaimAttachment receipt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "certificate_id", nullable = false)
    private ClaimAttachment certificate;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private Long reviewedById;

    @Column(length = 180)
    private String reviewedByName;

    @Column(length = 1000)
    private String managerReason;

    private LocalDate paymentDate;

    private LocalDateTime paidAt;

    private Long paidById;

    @Column(length = 180)
    private String paidByName;

    @Column(length = 100)
    private String paymentReference;

    @Column(length = 1000)
    private String paymentNote;

    public CourseFeeClaim() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public CourseApplication getApplication() {
        return application;
    }

    public void setApplication(CourseApplication application) {
        this.application = application;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public boolean isPaidPersonally() {
        return paidPersonally;
    }

    public void setPaidPersonally(boolean paidPersonally) {
        this.paidPersonally = paidPersonally;
    }

    public ClaimAttachment getReceipt() {
        return receipt;
    }

    public void setReceipt(ClaimAttachment receipt) {
        this.receipt = receipt;
    }

    public ClaimAttachment getCertificate() {
        return certificate;
    }

    public void setCertificate(ClaimAttachment certificate) {
        this.certificate = certificate;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Long getReviewedById() {
        return reviewedById;
    }

    public void setReviewedById(Long reviewedById) {
        this.reviewedById = reviewedById;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public void setReviewedByName(String reviewedByName) {
        this.reviewedByName = reviewedByName;
    }

    public String getManagerReason() {
        return managerReason;
    }

    public void setManagerReason(String managerReason) {
        this.managerReason = managerReason;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public Long getPaidById() {
        return paidById;
    }

    public void setPaidById(Long paidById) {
        this.paidById = paidById;
    }

    public String getPaidByName() {
        return paidByName;
    }

    public void setPaidByName(String paidByName) {
        this.paidByName = paidByName;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getPaymentNote() {
        return paymentNote;
    }

    public void setPaymentNote(String paymentNote) {
        this.paymentNote = paymentNote;
    }
}
