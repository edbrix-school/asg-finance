package com.asg.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BillDetailDto {
    private Long detRowId;
    private String billRef;
    private String remarks;
    private BigDecimal billAmount;
    private String createdBy;
    private LocalDate createdDate;
}