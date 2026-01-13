package com.asg.finance.entity;


import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "GL_PDC_CHQ_BATCH_HDR",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "GL_PDC_CHQ_BATCH_HDR_UK1",
                        columnNames = {"DOC_REF"}
                ),
                @UniqueConstraint(
                        name = "GL_PDC_CHQ_BATCH_HDR_UK2",
                        columnNames = {"TRANSACTION_DATE", "GROUP_POID", "COMPANY_POID",
                                "DIVISION_CODE", "BANK_POID", "CHQ_START_NO"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdcChqBatchHdrEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;


    @Column(name = "TRANSACTION_DATE")
    private Date transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Column(name = "PAY_GL_POID")
    private Long payGlPoid;

    @Column(name = "PAYING_TO", length = 100)
    private String payingTo;

    @Column(name = "PAYING_TYPE", length = 25)
    private String payingType;

    @Column(name = "DIVISION_CODE", length = 25)
    private String divisionCode;

    @Column(name = "BANK_POID")
    private Long bankPoid;

    @Column(name = "CHQ_START_NO", length = 20)
    private String chqStartNo;


    @Column(name = "CHQ_START_DATE")
    private Date chqStartDate;

    @Column(name = "CHQ_AMOUNT")
    private Double chqAmount;

    @Column(name = "NO_OF_CHQS")
    private Long noOfChqs;

    @Column(name = "TOTAL_AMOUNT")
    private Double totalAmount;

    @Column(name = "NARRATION", length = 1000)
    private String narration;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "BILL_TYPE", length = 30)
    private String billType;

    @Column(name = "BILL_REF", length = 50)
    private String billRef;

    @Column(name = "COST_GROUP", length = 100)
    private String costGroup;

    @Column(name = "COST_POID", length = 100)
    private String costPoid;

    @Column(name = "PRE_PRINTED", length = 1)
    private String prePrinted;

    @Column(name = "ACCOUNT_PAYEE", length = 1)
    private String accountPayee;

    @Column(name = "CONFIDENTIAL_REMARKS", length = 1000)
    private String confidentialRemarks;
}
