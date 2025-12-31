package com.asg.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "SHIP_PRINCIPAL_MASTER")
public class ShipPrincipalMaster {

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Id
    @Column(name = "PRINCIPAL_POID")
    private Long principalPoid;

    @Column(name = "PRINCIPAL_CODE", nullable = false, length = 20)
    private String principalCode;

    @Column(name = "PRINCIPAL_NAME", nullable = false, length = 100)
    private String principalName;

    @Column(name = "PRINCIPAL_NAME2", length = 100)
    private String principalName2;

    @Column(name = "COUNTRY_POID")
    private Long countryPoid;

    @Column(name = "ADDRESS_POID")
    private Long addressPoid;

    @Column(name = "CREDIT_PERIOD")
    private Long creditPeriod;

    @Column(name = "GL_CODE_POID")
    private Long glCodePoid;

    @Column(name = "REMARKS", length = 250)
    private String remarks;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "GROUP_NAME", length = 20)
    private String groupName;

    @Column(name = "GL_ACCTNO", length = 20)
    private String glAcctNo;

    @Column(name = "PRINCIPAL_CODE_OLD", length = 20)
    private String principalCodeOld;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private BigDecimal currencyRate;

    @Column(name = "AGREED_PERIOD")
    private Long agreedPeriod;

    @Column(name = "BUYING_RATE")
    private BigDecimal buyingRate;

    @Column(name = "SELLING_RATE")
    private BigDecimal sellingRate;

    @Column(name = "TIN_NUMBER", length = 100)
    private String tinNumber;

    @Column(name = "TAX_SLAB", length = 100)
    private String taxSlab;

    @Column(name = "EXEMPTION_REASON", length = 300)
    private String exemptionReason;
}