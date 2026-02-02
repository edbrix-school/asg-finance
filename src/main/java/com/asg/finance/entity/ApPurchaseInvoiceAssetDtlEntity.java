package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.finance.entity.key.ApPurchaseInvoiceAssetDtlKey;
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
@Table(name = "AP_PURCHASE_INVOICE_ASSET_DTL")
public class ApPurchaseInvoiceAssetDtlEntity {

    @EmbeddedId
    @AuditIgnore
    private ApPurchaseInvoiceAssetDtlKey id;

    @Column(name = "FA_CODE")
    private String faCode;

    @Column(name = "FA_DESCRIPTION")
    private String faDescription;

    @AuditIgnore
    @Column(name = "FA_CATEGORY")
    private String faCategory;

    @Column(name = "ASSET_TYPE")
    private String assetType;

    @Column(name = "VALUE")
    private Long value;

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
}
