package com.asg.finance.entity;


import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.finance.entity.key.ApPurchaseInvoiceItemDtlKey;
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
@Table(name = "AP_PURCHASE_INVOICE_ITEM_DTL")
public class ApPurchaseInvoiceItemDtlEntity {

    @EmbeddedId
    @AuditIgnore
    private ApPurchaseInvoiceItemDtlKey id;

    @AuditIgnore
    @Column(name = "STOCK_POID")
    private Long stockPoid;

    @AuditIgnore
    @Column(name = "STOCK_UNIT_POID")
    private Long stockUnitPoid;

    @Column(name = "PO_QTY")
    private BigDecimal poQty;

    @AuditIgnore
    @Column(name = "DN_QTY")
    private Long dnQty;

    @AuditIgnore
    @Column(name = "QTY_RECEIVED")
    private Long qtyReceived;

    @Column(name = "PRICE")
    private BigDecimal price;

    @Column(name = "DISCOUNT")
    private BigDecimal discount;

    @Column(name = "TOTAL")
    private BigDecimal total;

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
    @Column(name = "REF_DOC_ID")
    private String refDocId;

    @AuditIgnore
    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;

    @Column(name = "CHECK_ALL")
    private String checkAll;

    @AuditIgnore
    @Column(name = "REF_DET_ROW_ID")
    private Long refDetRowId;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE")
    private BigDecimal taxPercentage;

    @Column(name = "TAX_AMOUNT")
    private BigDecimal taxAmount;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "BASE_AMOUNT")
    private BigDecimal baseAmount;
}
