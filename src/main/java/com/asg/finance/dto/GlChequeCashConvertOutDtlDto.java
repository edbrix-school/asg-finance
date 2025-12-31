package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GlChequeCashConvertOutDtlDto {

    private Long transactionPoid;
    private Long detRowId;
    private Long paymentMainPoid;
    private Long amount;
    private String remarks;
    private Long bankPoid;
    private String chqAcName;
    private String chqAcNo;
    private String chqCardNo;
    private LocalDate chqDate;
    private String selected;
    private String voucherType;
    private String lineType;

    private LovGetListDto paymentMainDet;
    private LovGetListDto bankDet;
}
