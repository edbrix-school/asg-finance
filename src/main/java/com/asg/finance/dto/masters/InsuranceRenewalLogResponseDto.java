package com.asg.finance.dto.masters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceRenewalLogResponseDto {
    private Long detRowId;
    private LocalDate renewalDate;
    private LocalDate fromDate;
    private LocalDate expiryDate;
    private BigDecimal insuranceAmount;
    private BigDecimal premiumAmount;
    private String createdBy;
    private String lastModifiedBy;
}
