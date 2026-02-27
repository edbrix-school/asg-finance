package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ApPurchaseInvRjvDetailsDto {
    private Long transactionPoid;
    private Long detRowId;

    private String drilldownLinkInfo;
    private Long rjvPoid;
    private LocalDate rjvTrnDate;
    private String rjvDocRef;
    private Long rjvCompanyPoid;
    private String rjvRefType;
    private BigDecimal rjvAmount;
    private String rjvRemarks;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    private LovGetListDto rjvCompanyDet;

    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"
}
