package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApPurchaseInvoiceItemDtlDto {

    private Long transactionPoid;
    private Long detRowId;

    private Long stockPoid;
    private Long stockUnitPoid;
    private Long poQty;
    private Long dnQty;
    private Long qtyReceived;
    private Long price;
    private Long discount;
    private Long total;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String refDocId;
    private Long refDocPoid;
    private String checkAll;
    private Long refDetRowId;
    private Long taxPoid;
    private Long taxPercentage;
    private Long taxAmount;
    private Long amount;
    private Long baseAmount;

    private LovGetListDto stockDet;
    private LovGetListDto stockUnitDet;
    private LovGetListDto taxDet;
    private LovGetListDto refDocDet;

    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"
}
