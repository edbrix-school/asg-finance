package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringJvScheduleDetailResponse {

    private Long detRowId;
    private LocalDate monthWiseDate;
    private String jv;
    private BigDecimal amount;
    private String status;
    private String remarks;
    private String drilldownLinkInfo;
    private Integer seqno;
}

