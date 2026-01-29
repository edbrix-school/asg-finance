package com.asg.finance.dto;

import com.asg.finance.config.ThreeDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApPurchaseJournalResponseDto {

    private Long chargePoid;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double chargeBaseAmount;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double ffAmount;
    private String refDocId;
    private String refDocPoid;
    private Long fdaDetRowId;
    private Long taxPoid;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double taxPercentage;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double taxAmount;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double chargeAmount;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private Double pdaAmount;
    private String remarks;

}
