package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalVoucherCapitalizationDto {
    @NotNull(message = "Asset poid is required")
    private Long faPoid;

    @NotNull(message = "Asset value is required")
    private BigDecimal assetValue;
    private LovGetListDto fixedAssetCategoryDet;
    private Long detRowId;
    private String actionType;
    private String faDescription;
    private Long faCategory;
    private LovGetListDto assetTypeDet;
    private String assetType;
    private String remarks;
}
