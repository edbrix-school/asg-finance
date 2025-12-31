package com.asg.finance.entity;

import com.asg.finance.entity.key.GlBankPaymentItemDtlId;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "GL_BANK_PAYMENT_ITEM_DTL")
@IdClass(GlBankPaymentItemDtlId.class)
public class GlBankPaymentItemDtlEntity implements Serializable {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

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

    @Column(name = "REMARKS", length = 100)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "REF_DOC_ID", length = 100)
    private String refDocId;

    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;

    @Column(name = "REF_DET_ROW_ID")
    private Long refDetRowId;
}