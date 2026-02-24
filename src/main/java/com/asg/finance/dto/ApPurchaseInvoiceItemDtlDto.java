package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Data;

import java.math.BigDecimal;
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
    private BigDecimal price;
    private Long discount;
    private BigDecimal total;
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
    private BigDecimal taxAmount;
    private BigDecimal amount;
    private BigDecimal baseAmount;

    private LovGetListDto stockDet;
    private LovGetListDto stockUnitDet;
    private LovGetListDto taxDet;
    private LovGetListDto refDocDet;

    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"
}
