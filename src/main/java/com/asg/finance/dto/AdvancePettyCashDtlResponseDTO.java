package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvancePettyCashDtlResponseDTO {
    
    private Long detRowId;
    private Long transactionPoid;
    private LocalDate documentDate;
    private String pettyCashReference;
    private String drilldownLinkInfo;
    private BigDecimal amount;
    private String remarks;
}