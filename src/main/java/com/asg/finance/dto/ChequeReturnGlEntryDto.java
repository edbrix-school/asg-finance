package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Data;

import java.util.Date;

@Data
public class ChequeReturnGlEntryDto {

    private String type;
    private Long companyPoid;
    private LovGetListDto companyDtl;
    private Long glPoid;
    private LovGetListDto glDtl;
    private Double amount;
}
