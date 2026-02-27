package com.asg.finance.dto.masters;

import com.asg.common.lib.dto.DetailsDto;
import com.asg.common.lib.dto.LovGetListDto;
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
    private Long detRowId;
    private DetailsDto role;
    private String contactType;
    private LovGetListDto contactTypeDetails;
    private LovGetListDto picPerson;
    private LocalDate fromDate;
    private LocalDate toDate;
}