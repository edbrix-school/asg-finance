package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_BANK_FILE_HDR")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlBankFileHdr {
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

    @Column(name = "DOC_REF", length = 100, insertable = false, updatable = false)
    @Generated
    private String docRef;

    @Column(name = "BANK_POID")
    private Long bankPoid;

    @Column(name = "BANK_LIST", length = 1)
    private String bankList;

    @Column(name = "LONG_NARRATION", length = 500)
    private String longNarration;

    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 50)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "VALUE_DATE")
    private LocalDate valueDate;

    @Column(name = "FILE_NAME", length = 100)
    private String fileName;

    @Column(name = "ONLY_APPROVAL", length = 25)
    private String onlyApproval;

    @Column(name = "TT_SUPPRESS_BALANCE_CHECK", length = 25)
    private String ttSuppressBalanceCheck;
}