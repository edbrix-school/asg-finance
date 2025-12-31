package com.asg.finance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreditNoteCostCenterBreakupPopupDto {

    private Long id;
    private Long glDetRowId;
    private String costCenterCode;
    private Long costCenterPoid;
    private BigDecimal amount;
    private String remarks;
}
