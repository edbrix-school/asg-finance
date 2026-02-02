package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "GL_MASTER_PYMT_DTL")
@IdClass(GLPaymentDetailsEntity.CompositeKey.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GLPaymentDetailsEntity {

    @Id
    @Column(name = "GL_POID", nullable = false)
    @AuditIgnore
    private Long glPoid;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pymtDtlSeq")
    @SequenceGenerator(name = "pymtDtlSeq", sequenceName = "SEQ_GL_MASTER_PYMT_DTL", allocationSize = 1)
    @Column(name = "DET_ROW_ID", nullable = false)
    @AuditIgnore
    private Long id;

    @Column(name = "BANK", length = 100)
    private String bank;

    @Column(name = "SWIFT_CODE", length = 50)
    private String swiftCode;

    @Column(name = "ACCOUNT_NUMBER", length = 50)
    private String accountNumber;

    @Column(name = "TYPE", length = 30)
    private String type;

    @Column(name = "BENEFICIARY_NAME", length = 80)
    private String beneficiaryName;

    @Column(name = "ADDRESS", length = 250)
    private String address;

    @Column(name = "BANK_ADDRESS", length = 250)
    private String bankAddress;

    @Column(name = "BANK_SWIFT_CODE", length = 50)
    private String bankSwiftCode;

    @Column(name = "IBAN", length = 100)
    private String iban;

    @Column(name = "INTERMEDIARY_BANK", length = 100)
    private String intermediaryBank;

    @Column(name = "BENEFICIARY_ID", length = 25)
    private String beneficiaryId;

    @Column(name = "INTERMEDIARY_ACCT", length = 50)
    private String intermediaryAcct;

    @Column(name = "INTERMEDIARY_OTH", length = 50)
    private String intermediaryOth;

    @Column(name = "SPECIAL_INSTRUCTION", length = 250)
    @AuditIgnore
    private String specialInstruction;

    @Column(name = "INTERMEDIARY_COUNTRY_POID")
    private Long intermediaryCountryPoid;

    @Column(name = "BENEFICIARY_COUNTRY")
    private Long beneficiaryCountry;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "DEFAULTS", length = 1)
    @AuditIgnore
    private String defaults;

    @Column(name = "CREATED_BY", length = 20)
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    @AuditIgnore
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private LocalDateTime lastModifiedDate;

    @Column(name = "REMARKS", length = 250)
    @AuditIgnore
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GL_POID", nullable = false, insertable = false, updatable = false)
    private GLMasterEntity glMaster;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long glPoid;  // Position 1 - matches database constraint order
        private Long id;      // Position 2 (DET_ROW_ID)
    }
}
