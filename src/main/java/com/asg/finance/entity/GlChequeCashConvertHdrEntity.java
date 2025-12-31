package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.Data;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DOC_REF")
    private String docRef;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "POSTING_NARRATION")
    private String postingNarration;

    @Column(name = "CASH", precision = 19, scale = 2)
    private Long cash;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED")
    private String deleted;

    @Column(name = "CHQ_AC_NO")
    private String chqAcNo;

    @Column(name = "CHQ_CARDNO")
    private String chqCardNo;

    @Column(name = "ROUNDING_AMT")
    private Long roundingAmt;
}
