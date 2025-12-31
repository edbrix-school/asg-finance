package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_RECURRING_JV_HDR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlRecurringJvHdr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private BigDecimal currencyRate;

    @Column(name = "NARRATION", length = 250)
    private String narration;

    @Column(name = "REFERENCE_NO", length = 100)
    private String referenceNo;

    @Column(name = "TOTAL_AMOUNT")
    private BigDecimal totalAmount;

    @Column(name = "START_DATE")
    private LocalDate startDate;

    @Column(name = "NO_OF_MONTHS")
    private Integer noOfMonths;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

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

    @Column(name = "MONTH_WISE_AMT")
    private BigDecimal monthWiseAmt;

    @Column(name = "REF_TYPE", length = 300)
    private String refType;

    @Column(name = "EMPLOYEE_POID")
    private Long employeePoid;

    @Column(name = "FA_POID")
    private Long faPoid;

    @Column(name = "POLICY_NUMBER", length = 500)
    private String policyNumber;
}
