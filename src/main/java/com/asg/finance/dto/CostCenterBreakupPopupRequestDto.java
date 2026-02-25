package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCenterBreakupPopupRequestDto {
    private Long costDetRowId;
    private String costGroup;
    private String costPoid;
    private BigDecimal amount;
    private LovGetListDto costCenterDetails;
    
    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"
}
