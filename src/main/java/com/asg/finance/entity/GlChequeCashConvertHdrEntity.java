package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "GL_CHEQUE_CASH_CONVERT_HDR",
        uniqueConstraints = @UniqueConstraint(
                name = "GL_CHEQUE_CASH_CONVERT_HD_UK1",
                columnNames = {"GROUP_POID", "COMPANY_POID"}
        )
)
@Data
public class GlChequeCashConvertHdrEntity {

    @Id
    @AuditIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    @AuditIgnore
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    @AuditIgnore
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    @AuditIgnore
    private Long companyPoid;

    @Column(name = "DOC_REF")
    private String docRef;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "POSTING_NARRATION")
    private String postingNarration;

    @Column(name = "CASH", precision = 19, scale = 2)
    private BigDecimal cash;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "CREATED_BY")
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    @AuditIgnore
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED")
    private String deleted;

    @Column(name = "CHQ_AC_NO")
    private String chqAcNo;

    @Column(name = "CHQ_CARDNO")
    private String chqCardNo;

    @Column(name = "ROUNDING_AMT")
    @AuditIgnore
    private BigDecimal roundingAmt;
}
