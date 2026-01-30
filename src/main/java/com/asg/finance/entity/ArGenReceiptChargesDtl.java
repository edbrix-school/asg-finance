package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "AR_GEN_RECEIPT_CHARGES_DTL")
@IdClass(ArGenReceiptChargesDtl.CompositeKey.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArGenReceiptChargesDtl {

    @Id
    @AuditIgnore
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @AuditIgnore
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", nullable = false, insertable = false, updatable = false)
    @AuditIgnore
    private ArGenReceiptHdr receiptHdr;

    @Column(name = "CHARGE_TYPE", length = 300)
    private String chargeType;

    @Column(name = "GL_POID")
    private Long glPoid;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "BHD_AMOUNT")
    private BigDecimal bhdAmount;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE")
    private BigDecimal taxPercentage;

    @Column(name = "TAX_AMOUNT")
    private BigDecimal taxAmount;

    @Column(name = "TOTAL_AMOUNT")
    private BigDecimal totalAmount;

    @Column(name = "COST_POID", length = 300)
    private String costPoid;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long transactionPoid;
        private Long detRowId;
    }
}

