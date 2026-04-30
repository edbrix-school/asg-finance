package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BankPaymentItemDetailResponse {

    private Long transactionPoid;
    private Long detRowId;
    private Long stockPoid;
    private LovGetListDto stockDet;
    private Long stockUnitPoid;
    private LovGetListDto stockUnitDet;
    private Double poQty;
    private Double dnQty;
    private Double qtyReceived;
    private Double price;
    private Double discount;
    private Double total;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private LovGetListDto refDocDet;
    private Long refDetRowId;
    private String createdBy;
    private LocalDateTime createdDate;
}