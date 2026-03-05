package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeTaxDataDto {
    private Long taxPoid;
    private LovGetListDto taxDet;
    private BigDecimal percentage;
}
