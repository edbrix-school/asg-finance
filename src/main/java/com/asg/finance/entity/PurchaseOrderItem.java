package com.asg.finance.entity;

import com.asg.finance.entity.master.UnitMaster;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@IdClass(PurchaseOrderItem.CompositeKey.class)
@Table(name = "AP_PURCHASE_ORDER_ITEM_DTL")
public class PurchaseOrderItem {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "TRANSACTION_POID",
            referencedColumnName = "TRANSACTION_POID",
            insertable = false,
            updatable = false
    )
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "STOCK_POID",
            referencedColumnName = "STOCK_POID",
            insertable = false,
            updatable = false
    )
    private StockMasterEntity stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "STOCK_UNIT_POID",
            referencedColumnName = "STOCK_UNIT_POID",
            insertable = false,
            updatable = false
    )
    private UnitMaster stockUnit;

    @Column(name = "STOCK_POID")
    private Long stockPoid;

    @Column(name = "STOCK_UNIT_POID")
    private Long stockUnitPoid;

    @Column(name = "QTY")
    private Double qty;

    @Column(name = "PRICE")
    private Double price;

    @Column(name = "DISCOUNT")
    private Double discount;

    @Column(name = "TOTAL")
    private Double total;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "RFQ_DET_ROW_ID")
    private Long rfqDetRowId;

    @Column(name = "RFQ_POID")
    private Long rfqPoid;

    @Column(name = "PUR_REQ_DET_ROW_ID")
    private Long purReqDetRowId;

    @Column(name = "PUR_REQ_POID")
    private Long purReqPoid;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE")
    private Double taxPercentage;

    @Column(name = "TAX_AMOUNT")
    private Double taxAmount;

    @Column(name = "ITEM_DTL_READ_ONLY", length = 1)
    private String itemDtlReadOnly;

    @Column(name = "PJ_DET_ROW_ID")
    private Long pjDetRowId;

    @Column(name = "PJ_POID")
    private Long pjPoid;

    @Column(name = "BASE_AMOUNT")
    private Double baseAmount;

    @Column(name = "PO_IMP_DET_ROW_ID")
    private Long poImpDetRowId;

    @Column(name = "DISCOUNT_PERCENTAGE")
    private Double discountPercentage;

    @Column(name = "LAST_PUR_PRICE")
    private Double lastPurPrice;

    @Column(name = "CONVERTED_QTY")
    private Double convertedQty;

    @Column(name = "CONVERTED_UNIT")
    private Double convertedUnit;

    @Column(name = "CONVERSION_VALUE")
    private Double conversionValue;


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class  CompositeKey implements Serializable {

        private Long transactionPoid;
        private Long detRowId;
    }

}
