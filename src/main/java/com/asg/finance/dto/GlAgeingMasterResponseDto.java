package com.asg.finance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlAgeingMasterResponseDto {
    private String status;
    private Long ageingPoid;
}
