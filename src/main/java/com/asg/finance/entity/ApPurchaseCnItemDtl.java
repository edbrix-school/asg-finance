package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "AP_PURCHASE_CN_ITEM_DTL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(com.asg.finance.entity.key.ApPurchaseCnItemDtlKey.class)
public class ApPurchaseCnItemDtl {

    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "STOCK_POID")
    private Long stockPoid;

    @Column(name = "STOCK_UNIT_POID")
    private Long stockUnitPoid;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "PRICE")
    private BigDecimal price;

    @Column(name = "DISCOUNT")
    private BigDecimal discount;

    @Column(name = "TOTAL")
    private BigDecimal total;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "REF_DOC_ID", length = 100)
    private String refDocId;

    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;

    @Column(name = "CHECK_ALL", length = 1)
    private String checkAll;

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

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastModifiedDate;
}
