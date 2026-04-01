package com.asg.finance.dto;

import com.asg.common.lib.dto.DetailsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PettyCashFromGrnDto {
    private Long transactionPoid;
    private String transactionDate;
    private String docRef;
    private Long companyPoid;
    private Long supplierPoid;
    private DetailsDto supplierPoidDtl;
    private Long locationPoid;
    private DetailsDto locationPoidDtl;
    private String remarks;
    private BigDecimal grandTotal;
    private String drilldownLinkInfo;
}
