package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItemRequestDto {

    private Long detRowId;

    private Long stockPoid;
    private Long stockUnitPoid;

    private Double qty;
    private Double price;
    private Double discount;
    private Double total;

    private String remarks;

    private Long rfqDetRowId;
    private Long rfqPoid;

    private Long purReqDetRowId;
    private Long purReqPoid;

    private Long taxPoid;
    private Double taxPercentage;
    private Double taxAmount;

    private String itemDtlReadOnly;

    private Long pjDetRowId;
    private Long pjPoid;

    private Double baseAmount;

    private Long poImpDetRowId;

    private Double discountPercentage;
    private Double lastPurPrice;

    private Double convertedQty;
    private Double convertedUnit;
    private Double conversionValue;

    private String actionType;
}
