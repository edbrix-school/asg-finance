package com.asg.finance.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayGlBreakupCheckResponseDto {
    private String result;        // BILL_WISE | COST_GROUP | NODATA | NODATA :No Data
    private String costGroup;
}
