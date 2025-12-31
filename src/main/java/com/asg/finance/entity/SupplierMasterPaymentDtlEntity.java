package com.asg.finance.entity;

import com.asg.finance.entity.key.SupplierPaymentDetailId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "AP_SUPPLIER_MASTER_PYMT_DTL")
public class SupplierMasterPaymentDtlEntity {
    @EmbeddedId
    private SupplierPaymentDetailId id;

    @Column(name = "BANK")
    private String bank;

    @Column(name = "SWIFT_CODE")
    private String swiftCode;

    @Column(name = "ACCOUNT_NUMBER")
    private String accountNumber;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "BENEFICIARY_NAME")
    private String beneficiaryName;

    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "BANK_ADDRESS")
    private String bankAddress;

    @Column(name = "BANK_SWIFT_CODE")
    private String bankSwiftCode;

    @Column(name = "IBAN")
    private String iban;

    @Column(name = "INTERMEDIARY_BANK")
    private String intermediaryBank;

    @Column(name = "BENEFICIARY_ID")
    private String beneficiaryId;

    @Column(name = "INTERMEDIARY_ACCT")
    private String intermediaryAcct;

    @Column(name = "INTERMEDIARY_OTH")
    private String intermediaryOth;

    @Column(name = "SPECIAL_INSTRUCTION")
    private String specialInstruction;

    @Column(name = "INTERMEDIARY_COUNTRY_POID")
    private Long intermediaryCountryPoid;

    @Column(name = "BENEFICIARY_COUNTRY")
    private Long beneficiaryCountry;

    @Column(name = "ACTIVE")
    private String active;

    @Column(name = "DEFAULTS")
    private String defaults;
}