package com.asg.finance.entity;

import com.asg.finance.entity.key.ApPurchaseInvoiceGlDtlKey;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "AP_PURCHASE_INVOICE_GL_DTL")
public class ApPurchaseInvoiceGlDtlEntity {

    @EmbeddedId
    private ApPurchaseInvoiceGlDtlKey id;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "GL_POID")
    private Long glPoid;

    @Column(name = "DR_AMOUNT")
    private Long drAmount;

    @Column(name = "CR_AMOUNT")
    private Long crAmount;

    @Column(name = "REF_DOC_ID")
    private String refDocId;

    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "JOB_NO_OLD")
    private String jobNoOld;

    @Column(name = "MOD_CODE_OLD")
    private String modCodeOld;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE")
    private Long taxPercentage;

    @Column(name = "TAX_AMOUNT")
    private Long taxAmount;

    @Column(name = "TOTAL_AMOUNT")
    private Long totalAmount;
}
