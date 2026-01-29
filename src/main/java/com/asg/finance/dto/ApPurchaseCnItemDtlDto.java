package com.asg.finance.dto;

import com.asg.finance.config.ThreeDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class ApPurchaseCnItemDtlDto {
    private Long detRowId;
    private Long stockPoid;
    private Long stockUnitPoid;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal quantity;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal price;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal discount;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal total;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private String checkAll;
    private Long refDetRowId;
    private Long taxPoid;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal taxPercentage;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal taxAmount;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal amount;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal baseAmount;
    
    // Action type field
    private String actionType; // isCreated, isUpdated, isDeleted
}
