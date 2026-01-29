package com.asg.finance.dto;

import com.asg.finance.config.ThreeDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItemResponseDto {
    private Long transactionPoid;   // PK part-1
    private Long detRowId;          // PK part-2

    private Long stockPoid;
    private Long stockUnitPoid;

    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double qty;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double price;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double discount;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double total;

    private String remarks;

    private Long rfqDetRowId;
    private Long rfqPoid;

    private Long purReqDetRowId;
    private Long purReqPoid;

    private Long taxPoid;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double taxPercentage;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double taxAmount;

    private String itemDtlReadOnly;

    private Long pjDetRowId;
    private Long pjPoid;

    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double baseAmount;

    private Long poImpDetRowId;

    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double discountPercentage;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double lastPurPrice;

    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double convertedQty;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double convertedUnit;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double conversionValue;
}
