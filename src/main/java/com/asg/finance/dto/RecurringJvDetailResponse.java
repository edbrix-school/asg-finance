package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringJvDetailResponse {

    private Long lineId;
    private String type;
    private Long companyId;
    private Long glId;
    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private String remarks;

    private LovGetListDto glDet;

    private List<CostCenterBreakupPopupRequestDto> costCenter;
    private List<BillwiseBreakupPopupRequestDto> billWiseBreakup;

}

