package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "GL_BANK_MASTER",
        uniqueConstraints = {
                @UniqueConstraint(name = "GL_BANK_MAST_UK_CODE", columnNames = "BANK_CODE"),
                @UniqueConstraint(name = "GL_BANK_MAST_UK_DESC", columnNames = "BANK_DESCRIPTION")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlBankEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BANK_POID")
    private Long bankPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "BANK_CODE", length = 20)
    private String bankCode;

    @Column(name = "BANK_DESCRIPTION", length = 100)
    private String bankDescription;

    @Column(name = "BANK_DESCRIPTION2", length = 100)
    private String bankDescription2;

    @Column(name = "GL_POID")
    private Long glPoid;

    @Column(name = "BANK_ACCOUNT_NO", length = 100)
    private String bankAccountNo;

    @Column(name = "IBAN", length = 100)
    private String iban;

    @Column(name = "SWIFT_CODE", length = 100)
    private String swiftCode;

    @Column(name = "BANK_ADDRESS", length = 500)
    private String bankAddress;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "BUYING_RATE", precision = 19, scale = 2)
    private BigDecimal buyingRate;

    @Column(name = "SELLING_RATE", precision = 19, scale = 2)
    private BigDecimal sellingRate;

    @Column(name = "PERIOD_START")
    private LocalDate periodStart;

    @Column(name = "PERIOD_END")
    private LocalDate periodEnd;

    @Column(name = "CARD_COMMISION", precision = 19, scale = 2)
    private BigDecimal cardCommision;

    @Column(name = "CHEQUE_PRINTING_YN", length = 1)
    private String chequePrintingYn;

    @Column(name = "SEQNO", precision = 5, scale = 0)
    private Integer seqno;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "OD_LIMIT", precision = 19, scale = 2)
    private BigDecimal odLimit;

    @Column(name = "COMPANY_POID", length = 20)
    private String companyPoid;

    @Column(name = "OLD_GLACCNO", length = 20)
    private String oldGlAccNo;

    @Column(name = "BANK_PREFIX", length = 20)
    private String bankPrefix;

    @Column(name = "ONLINE_FILE_TT", length = 1)
    private String onlineFileTt;

    @Column(name = "BANK_STATEMENT_DATE")
    private LocalDate bankStatementDate;

    @Column(name = "CURRENCY_RATE", precision = 19, scale = 2)
    private BigDecimal currencyRate;

    @Column(name = "VAT_TIN_NUMBER", length = 300)
    private String vatTinNumber;

    @Column(name = "ACCOUNT_TYPE", length = 100)
    private String accountType;

    @Column(name = "CORRESPONDANT_SWIFT_CODE", length = 100)
    private String correspondantSwiftCode;

    @Column(name = "CORRESPONDANT_BANK", length = 500)
    private String correspondantBank;

    @Column(name = "EDI_BANK_ACCOUNT_NO", length = 300)
    private String ediBankAccountNo;

}

