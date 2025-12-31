package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlPettyCashItemDtlRequestDto {

    private Long detRowId;

    private Long stockPoid;
    private Long stockUnitPoid;

    private BigDecimal poQty;
    private BigDecimal dnQty;
    private BigDecimal qtyReceived;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal total;

    private String remarks;


    private String refDocId;
    private Long refDocPoid;
    private String checkAll;
    private Long refDetRowId;

    private String vatPartyName;
    private String partyInvNumber;
    private LocalDate partyInvDate;
    private Long taxPoid;
    
    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"
}
