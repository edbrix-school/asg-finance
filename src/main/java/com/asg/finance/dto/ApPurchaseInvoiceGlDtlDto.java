package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApPurchaseInvoiceGlDtlDto {

    private Long transactionPoid;
    private Long detRowId;

    private String type;
    private Long companyPoid;
    private Long glPoid;
    private Long drAmount;
    private Long crAmount;
    private String refDocId;
    private Long refDocPoid;
    private String description;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String jobNoOld;
    private String modCodeOld;
    private Long taxPoid;
    private Long taxPercentage;
    private Long taxAmount;
    private Long totalAmount;

    private LovGetListDto companyDet;
    private LovGetListDto glDet;
    private LovGetListDto refDocDet;
    private LovGetListDto taxDet;

    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"

    private List<BillwiseBreakupPopupRequestDto> billwiseBreakupList;
    private List<CostCenterBreakupPopupRequestDto> costCenterBreakupList;
}
