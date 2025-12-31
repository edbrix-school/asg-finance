package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "GLOBAL_TAX_SUBMISSION_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(GlobalTaxSubmissionDtlId.class)
public class GlobalTaxSubmissionDtl {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "TAX_TYPE", length = 300)
    private String taxType;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_CODE", length = 100)
    private String taxCode;

    @Column(name = "TAX_DESCRIPTION", length = 500)
    private String taxDescription;

    @Column(name = "TAX_PERCENTAGE", length = 100)
    private String taxPercentage;

    @Column(name = "TAX_BASE_AMOUNT", precision = 18, scale = 3)
    private BigDecimal taxBaseAmount;

    @Column(name = "TAX_AMOUNT", precision = 18, scale = 3)
    private BigDecimal taxAmount;

    @Column(name = "TOTAL_AMOUNT", precision = 18, scale = 3)
    private BigDecimal totalAmount;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "CREATED_BY", length = 30)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 30)
    private String lastmodifiedBy;

    @UpdateTimestamp
    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastmodifiedDate;
}


