package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.finance.entity.key.ApPurchaseInvoiceGlDtlKey;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "AP_PURCHASE_INVOICE_GL_DTL")
public class ApPurchaseInvoiceGlDtlEntity {

    @EmbeddedId
    @AuditIgnore
    private ApPurchaseInvoiceGlDtlKey id;

    @Column(name = "TYPE")
    private String type;

    @AuditIgnore
    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "GL_POID")
    private Long glPoid;

    @Column(name = "DR_AMOUNT")
    private BigDecimal drAmount;

    @Column(name = "CR_AMOUNT")
    private BigDecimal crAmount;

    @AuditIgnore
    @Column(name = "REF_DOC_ID")
    private String refDocId;

    @AuditIgnore
    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;

    @AuditIgnore
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "REMARKS")
    private String remarks;

    @AuditIgnore
    @Column(name = "CREATED_BY")
    private String createdBy;

    @AuditIgnore
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @AuditIgnore
    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @AuditIgnore
    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @AuditIgnore
    @Column(name = "JOB_NO_OLD")
    private String jobNoOld;

    @AuditIgnore
    @Column(name = "MOD_CODE_OLD")
    private String modCodeOld;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE")
    private Long taxPercentage;

    @Column(name = "TAX_AMOUNT")
    private BigDecimal taxAmount;

    @Column(name = "TOTAL_AMOUNT")
    private BigDecimal totalAmount;
}
