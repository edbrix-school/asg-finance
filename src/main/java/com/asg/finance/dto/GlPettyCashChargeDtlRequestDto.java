package com.asg.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlPettyCashChargeDtlRequestDto {
    private Long transactionPoid;
    private Long detRowId;
    private Long chargePoid;

    private BigDecimal chargeAmount;
    private String description;
    private String remarks;

    private String refDocId;
    private Long refDocPoid;
    private Long fdaDetRowId;
    private String checkAll;

    private BigDecimal pdaAmount;
    private BigDecimal ffAmount;
    private String chargeFrom;

    private String vatPartyName;
    private String partyInvNumber;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date partyInvDate;
    
    private Long taxPoid;
    
    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"
}
