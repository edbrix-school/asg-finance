package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "SALES_CUSTOMER_MASTER",
        uniqueConstraints = {
                @UniqueConstraint(name = "SALES_CUSTOMER_MASTER_UK_CODE", columnNames = "CUSTOMER_CODE"),
                @UniqueConstraint(name = "SALES_CUSTOMER_MASTER_UK_NAME", columnNames = "CUSTOMER_NAME")
        }
)
@Getter
@Setter
public class SalesCustomerMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
    @SequenceGenerator(name = "customer_seq", sequenceName = "SALES_CUSTOMER_MASTER_SEQ", allocationSize = 1)
    @Column(name = "CUSTOMER_POID", nullable = false)
    private Long customerPoid;

    @Column(name = "CUSTOMER_CODE", length = 20)
    private String customerCode;

    @Column(name = "CUSTOMER_NAME", length = 100)
    private String customerName;

    @Column(name = "CUSTOMER_NAME2", length = 100)
    private String customerName2;

    @Column(name = "CUSTOMER_CATEGORY_POID")
    private Long customerCategoryPoid;

    @Column(name = "ADDRESS_POID")
    private Long addressPoid;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CREDIT_LIMIT", precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "CREDIT_PERIOD")
    private Integer creditPeriod;

    @Column(name = "SALESMAN_POID")
    private Long salesmanPoid;

    @Column(name = "CR_REGNO", length = 50)
    private String crRegno;

    @Column(name = "CONTRACT_EXPIRY", length = 1)
    private String contractExpiry;

    @Column(name = "CONTRACT_EXPIRY_REASON", length = 200)
    private String contractExpiryReason;

    @Column(name = "BLOCKED_CUSTOMER", length = 1)
    private String blockedCustomer = "N";

    @Column(name = "BLOCKED_REASON", length = 200)
    private String blockedReason;

    @Column(name = "PAYMENT_TERMS", length = 100)
    private String paymentTerms;

    @Column(name = "DELIVERY_TERMS", length = 100)
    private String deliveryTerms;

    @Column(name = "ACCOUNT_NAME", length = 100)
    private String accountName;

    @Column(name = "BANK_AC", length = 50)
    private String bankAc;

    @Column(name = "SWIFT_CODE", length = 50)
    private String swiftCode;

    @Column(name = "IBAN", length = 50)
    private String iban;

    @Column(name = "ACCOUNT_NO", length = 50)
    private String accountNo;

    @Column(name = "GL_ACCT", length = 20)
    private String glAcct;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "CREDIT_TYPE", length = 20)
    private String creditType;

    @Column(name = "AUTHO_SIGNATORY1", length = 300)
    private String authoSignatory1;

    @Column(name = "AUTHO_SIGNATORY1_DESG", length = 300)
    private String authoSignatory1Desg;

    @Column(name = "AUTHO_SIGNATORY2", length = 300)
    private String authoSignatory2;

    @Column(name = "AUTHO_SIGNATORY2_DESG", length = 300)
    private String authoSignatory2Desg;

    @Column(name = "AUTHO_SIGNATORY3", length = 300)
    private String authoSignatory3;

    @Column(name = "AUTHO_SIGNATORY3_DESG", length = 300)
    private String authoSignatory3Desg;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "CUSTOMER_TYPE", length = 20)
    private String customerType;

    @Column(name = "CURRENCY_RATE", precision = 19, scale = 6)
    private BigDecimal currencyRate;

    @Column(name = "RATE_EXPIRY_DATE")
    private LocalDate rateExpiryDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "SEQNO", precision = 5, scale = 0)
    private Integer seqNo;

    @Column(name = "CR_LIMIT_WARNING", length = 1)
    private String crLimitWarning = "Y";

    @Column(name = "CR_PERIOD_WARNING", length = 1)
    private String crPeriodWarning = "Y";

    @Column(name = "DISC_PERCENT", precision = 5, scale = 2)
    private BigDecimal discPercent;

    @Column(name = "GL_POID", nullable = false)
    private Long glPoid;

    @Column(name = "TERMS_CONDITIONS_POID")
    private Long termsConditionsPoid;

    @Column(name = "OLD_CRM_ACCTNO", length = 20)
    private String oldCrmAcctNo;

    @Column(name = "BILL_COMPANY")
    private Long billCompany;

    @Column(name = "TDR_CUSTOMER_NAME", length = 100)
    private String tdrCustomerName;

    @Column(name = "CONTRACT_START")
    private LocalDate contractStart;

    @Column(name = "CONTRACT_END")
    private LocalDate contractEnd;

    @Column(name = "CREDIT_VALIDATION", length = 1)
    private String creditValidation = "Y";

    @Column(name = "BUSSINESS_TYPES", length = 500)
    private String businessTypes;

    @Column(name = "TERMS_POID")
    private Long termsPoid;

    @Column(name = "IMPORT_CR_LIMIT", precision = 19, scale = 2)
    private BigDecimal importCrLimit;

    @Column(name = "IMPORT_CR_PERIOD")
    private Integer importCrPeriod;

    @Column(name = "EXPORT_CR_LIMIT", precision = 19, scale = 2)
    private BigDecimal exportCrLimit;

    @Column(name = "EXPORT_CR_PERIOD")
    private Integer exportCrPeriod;

    @Column(name = "INVOICE_DATE_FORMAT", length = 100)
    private String invoiceDateFormat = "DD-MON-RRRR";

    @Column(name = "BANK_DETAILS_CMP_MASTER", length = 10)
    private String bankDetailsCmpMaster = "NO";

    @Column(name = "AUTHO_SIGNATORY4", length = 300)
    private String authoSignatory4;

    @Column(name = "AUTHO_SIGNATORY4_DESG", length = 300)
    private String authoSignatory4Desg;

    @Column(name = "AUTHO_SIGNATORY5", length = 300)
    private String authoSignatory5;

    @Column(name = "AUTHO_SIGNATORY5_DESG", length = 300)
    private String authoSignatory5Desg;

    @Column(name = "SEND_AUTO_STATEMENT", length = 1)
    private String sendAutoStatement;

    @Column(name = "STATEMENT_DUE_DAYS")
    private Integer statementDueDays;

    @Column(name = "IS_AIRLINE_SUBAGENT", length = 1)
    private String isAirlineSubagent;

    @Column(name = "AIR_BILL_CREDIT_LIMIT", precision = 19, scale = 2)
    private BigDecimal airBillCreditLimit;

    @Column(name = "AIR_BILL_CREDIT_PERIOD")
    private Integer airBillCreditPeriod;

    @Column(name = "BANK_GUARANTEE", precision = 19, scale = 2)
    private BigDecimal bankGuarantee;

    @Column(name = "BANK_GUARANTEE_EXPIRY")
    private LocalDate bankGuaranteeExpiry;

    @Column(name = "TIN_NUMBER", length = 100)
    private String tinNumber;

    @Column(name = "TAX_SLAB", length = 100)
    private String taxSlab;

    @Column(name = "EXEMPTION_REASON", length = 300)
    private String exemptionReason;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "PRINT_ORIGINAL_CURRENCY", length = 1)
    private String printOriginalCurrency = "N";

    @Column(name = "CREDIT_AGRMNT_APPRVD", length = 10)
    private String creditAgreementApproved;

    @Column(name = "CREDIT_AGRMNT_APPRVD_BY")
    private Long creditAgreementApprovedBy;

    @Column(name = "CREDIT_AGRMNT_APPRVD_DATE")
    private LocalDate creditAgreementApprovedDate;

    @Column(name = "PRICE_TYPE", length = 100)
    private String priceType;
}

