package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingBillwiseBreakupDto {
    private String billRef;
    private LocalDate billDueDate;
    private String remarks;
    private BigDecimal balance;
}
