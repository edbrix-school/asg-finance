package com.asg.finance.entity.master;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GLOBAL_INSURANCE_RENEWAL_LOG")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(InsuranceDetailId.class)
public class InsuranceRenewalLog {
    @Id
    @AuditIgnore
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @AuditIgnore
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    @AuditIgnore
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private InsuranceMaster insuranceMaster;

    @Column(name = "RENEWAL_DATE")
    private LocalDate renewalDate;

    @Column(name = "FROM_DATE")
    private LocalDate fromDate;

    @Column(name = "EXPIRY_DATE")
    private LocalDate expiryDate;

    @Column(name = "INSURANCE_AMOUNT")
    private BigDecimal insuranceAmount;

    @Column(name = "PREMIUM_AMOUNT")
    private BigDecimal premiumAmount;

    @Column(name = "CREATED_BY", length = 20)
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    @AuditIgnore
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private LocalDateTime lastModifiedDate;
}