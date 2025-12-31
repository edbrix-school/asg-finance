package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlBankCommissionDtlDto {
    private Long DetRowId;
    private Long commissionGlPoid;
    private LovGetListDto commissionGlDet;
    private Date periodFrom;
    private Date periodTo;
    private BigDecimal commissionPercent;
    private Long taxPoid;
    private LovGetListDto taxPoidDet;
    private String taxName;
    private String taxCode;
    private BigDecimal taxPercentage;
    @Size(max = 500, message = "Remarks must be at most 500 characters")
    private String remarks;
    @Size(max = 500, message = "Card Type must be at most 500 characters")
    private String cardType;
    private String actionType;

}
