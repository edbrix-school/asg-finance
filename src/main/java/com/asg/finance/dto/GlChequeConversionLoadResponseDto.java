package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Data;

import java.util.Date;
@Data
public class GlChequeConversionLoadResponseDto {

    private Long paymentMainPoid;
    private Double amount;
    private Date choDate;
    private String chqCardNo;
    private Date chqDate;
    private Long bankPoid;
    private LovGetListDto bankDet;
    private String chqAcNo;
    private String chqAcName;
    private String remarks;
    private String voucherType;
    private String lineType;
}
