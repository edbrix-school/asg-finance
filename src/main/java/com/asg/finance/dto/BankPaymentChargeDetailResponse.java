package com.asg.finance.dto;

import lombok.Data;
import com.asg.common.lib.dto.LovGetListDto;
import java.time.LocalDateTime;

@Data
public class BankPaymentChargeDetailResponse {

    private Long transactionPoid;
    private Long detRowId;
    private Long chargePoid;
    private LovGetListDto chargeDet;
    private Long chargeAmount;
    private String description;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private LovGetListDto refDocDet;
    private Long fdaDetRowId;
    private String checkAll;
    private Long pdaAmount;
    private Long ffAmount;
    private String createdBy;
    private LocalDateTime createdDate;
}