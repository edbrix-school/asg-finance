package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
    public class GlChequeCashConvertInDtlDto {

    private Long transactionPoid;
    private Long detRowId;
    private Long bankPoid;
    private String chqAcName;
    private String chqAcNo;
    private String chqCardNo;
    private LocalDate chqDate;
    private Long amount;
    private String remarks;
    private String voucherType;
    private Long chequeCompanyPoid;
    private Long paymentMainPoid;
    private String lineType;
    private String pymtType;
    private Long ttBankPoid;
    private String ttRef;
    private Long cardPoid;
    private String cardType;
    private String creditCardRef;

    private LovGetListDto bankDet;
    private LovGetListDto chequeCompanyDet;
    private LovGetListDto paymentMainDet;
    private LovGetListDto ttBankDet;
    private LovGetListDto cardDet;
}
