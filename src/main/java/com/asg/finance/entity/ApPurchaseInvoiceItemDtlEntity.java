package com.asg.finance.entity;


import com.asg.finance.entity.key.ApPurchaseInvoiceItemDtlKey;
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
@Table(name = "AP_PURCHASE_INVOICE_ITEM_DTL")
public class ApPurchaseInvoiceItemDtlEntity {

    @EmbeddedId
    private ApPurchaseInvoiceItemDtlKey id;

    @Column(name = "STOCK_POID")
    private Long stockPoid;

    @Column(name = "STOCK_UNIT_POID")
    private Long stockUnitPoid;

    @Column(name = "PO_QTY")
    private Long poQty;

    @Column(name = "DN_QTY")
    private Long dnQty;

    @Column(name = "QTY_RECEIVED")
    private Long qtyReceived;

    @Column(name = "PRICE")
    private Long price;

    @Column(name = "DISCOUNT")
    private Long discount;

    @Column(name = "TOTAL")
    private Long total;

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

    @Column(name = "REF_DOC_ID")
    private String refDocId;

    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;

    @Column(name = "CHECK_ALL")
    private String checkAll;

    @Column(name = "REF_DET_ROW_ID")
    private Long refDetRowId;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE")
    private Long taxPercentage;

    @Column(name = "TAX_AMOUNT")
    private Long taxAmount;

    @Column(name = "AMOUNT")
    private Long amount;

    @Column(name = "BASE_AMOUNT")
    private Long baseAmount;
}
