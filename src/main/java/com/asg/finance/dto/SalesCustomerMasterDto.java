package com.asg.finance.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class SalesCustomerMasterDto {
    private Long customerPoid;
    private String customerCode;
    private String customerName;
    private String customerName2;
    private Long customerCategoryPoid;
    private Long addressPoid;
    private String currencyCode;
    private BigDecimal creditLimit;
    private Integer creditPeriod;
    private Long salesmanPoid;
    private String crRegno;
    private String contractExpiry;
    private String contractExpiryReason;
    private String blockedCustomer;
    private String blockedReason;
    private String paymentTerms;
    private String deliveryTerms;
    private String accountName;
    private String bankAc;
    private String swiftCode;
    private String iban;
    private String accountNo;
    private String glAcct;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String creditType;
    private String active;
    private String customerType;
    private BigDecimal currencyRate;
    private LocalDate rateExpiryDate;
    private String deleted;
}

