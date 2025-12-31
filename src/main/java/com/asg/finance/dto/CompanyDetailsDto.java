package com.asg.finance.dto;


import com.asg.common.lib.dto.LovGetListDto;
import lombok.Data;

@Data
public class CompanyDetailsDto {
    private Long detRowId;
    private Long glPoid;
    private Long companyPoid;
    private LovGetListDto companyDet;
    private String remarks;
    private String actionType;
}

