package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApPurchaseInvRjvDefaultDto {

    private String drilldownLinkInfo;
    private LocalDateTime rjvTrnDate;
    private String rjvDocRef;
    private Long rjvCompanyPoid;
    private String rjvRefType;
    private BigDecimal rjvAmount;
    private String rjvRemarks;
}
