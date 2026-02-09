package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "AR_GEN_RECEIPT_BILL_DTL")
@IdClass(ArGenReceiptBillDtl.CompositeKey.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArGenReceiptBillDtl {

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

    @Column(name = "GL_POID")
    @AuditIgnore
    private Long glPoid;

    @Column(name = "BILL_REF_TYPE", length = 100)
    private String billRefType;

    @Column(name = "BILL_REFNO", length = 500)
    @AuditIgnore
    private String billRefno;

    @Column(name = "BILL_DUE_DATE")
    private LocalDate billDueDate;

    @Column(name = "DESCRIPTION", length = 200)
    @AuditIgnore
    private String description;

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

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "CR_DR_TYPE", length = 20)
    private String crDrType;

    @Column(name = "CHECKALL", length = 1)
    private String checkall;

    @Column(name = "GL_COMPANY_POID")
    @AuditIgnore
    private Long glCompanyPoid;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long transactionPoid;
        private Long detRowId;
    }
}

