package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

@Entity
@Table(name = "AR_CREDIT_NOTE_HDR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArCreditNoteHdr {

    @Id
    @AuditIgnore
    @GeneratedValue(generator = "trigger-generated")
    @org.hibernate.annotations.GenericGenerator(
            name = "trigger-generated",
            strategy = "org.hibernate.id.IdentityGenerator"
    )
    @Column(name = "TRANSACTION_POID", updatable = false, insertable = false)
    private Long transactionPoid;

    @AuditIgnore
    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDate transactionDate;

    @AuditIgnore
    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @AuditIgnore
    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 25, nullable = false)
    private String docRef;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private BigDecimal currencyRate;

    @Column(name = "PARTY_TYPE", length = 100)
    private String partyType;

    @Column(name = "PARTY_POID")
    private Long partyPoid;

    @Column(name = "REF_TYPE", length = 100)
    private String refType;

    @Column(name = "FDA_REF", length = 100)
    private String fdaRef;

    @Column(name = "FF_REF", length = 100)
    private String ffRef;

    @Column(name = "TDR_REF", length = 100)
    private String tdrRef;

    @Column(name = "ADDRESS_POID")
    private Long addressPoid;

    @Column(name = "DR_TOTAL", precision = 19, scale = 2)
    private BigDecimal drTotal;

    @Column(name = "CR_TOTAL", precision = 19, scale = 2)
    private BigDecimal crTotal;

    @Column(name = "POSTING_NARRATION", length = 500)
    private String postingNarration;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "GRAND_TOTAL", precision = 19, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "SH_INVOICE_POID")
    private Long shInvoicePoid;

    @Column(name = "DN_INVOICE_POID")
    private Long dnInvoicePoid;

    @Column(name = "FF_INVOICE_POID")
    private Long ffInvoicePoid;

    @Column(name = "MTA_INVOICE_POID")
    private Long mtaInvoicePoid;

    @Column(name = "FDA_REF_POID")
    private Long fdaRefPoid;

    @Column(name = "TEMP_MEMNO", length = 21)
    private String tempMemno;

    @Column(name = "VOYAGE_REF", length = 50)
    private String voyageRef;

    @Column(name = "MULTI_COMPANY", length = 1)
    private String multiCompany = "N";

    @Column(name = "DUE_DATE")
    private LocalDate dueDate;

    @Column(name = "CREDIT_PERIOD")
    private Long creditPeriod;

    @Column(name = "FDA_DIRECT", length = 1)
    private String fdaDirect = "N";

    @Column(name = "TIN_NUMBER", length = 100)
    private String tinNumber;

    @Column(name = "OTHER_CURR_AMOUNT")
    private BigDecimal otherCurrAmount;

    @Column(name = "BHD_AMOUNT")
    private BigDecimal bhdAmount;

    @Column(name = "VOUCHER_TYPE", length = 100)
    private String voucherType;

    @Column(name = "CHANGE_POSTING", length = 1)
    private String changePosting;

    @Column(name = "BILL_REF_TYPE", length = 50)
    private String billRefType = "AGAINST";

    @Column(name = "BANK_POID")
    private Long bankPoid;

    @Column(name = "REMARKS_PRINTABLE", length = 1)
    private String remarksPrintable = "N";

    @Column(name = "PRINT_COMPANY_POID")
    private Long printCompanyPoid;

   /* @Column(name = "ISSUE_TYPE", length = 1)
    private String issueType;*/

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted = "N";
}