package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostCenterBreakupResponseDto {

    private Long mainDetRowId;
    private Long glPoid;
    private Long costDetRowId;
    private String costGroup;
    private String costPoid;
    private Long amount;
    private String description;

}
