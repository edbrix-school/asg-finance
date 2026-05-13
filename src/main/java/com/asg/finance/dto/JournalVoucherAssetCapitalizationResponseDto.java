package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JournalVoucherAssetCapitalizationResponseDto {
    private Long faPoid;
    private BigDecimal assetValue;
    private String faDescription;
    private Long faCategory;
    private LovGetListDto fixedAssetCategoryDet;
    private String assetType;
    private LovGetListDto assetTypeDet;
    private String remarks;
}
