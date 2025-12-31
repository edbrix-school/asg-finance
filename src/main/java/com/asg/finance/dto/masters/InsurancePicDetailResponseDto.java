package com.asg.finance.dto.masters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsurancePicDetailResponseDto {
    private Long rolePoid;
    private String contactType;
    private String picPerson;
    private LocalDate fromDate;
    private LocalDate toDate;
}