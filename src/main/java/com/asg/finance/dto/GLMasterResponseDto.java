package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GLMasterResponseDto {
    private Long glPoid;
    private String glCode;
    private String description;
    private String description2;
    private String type;
    private Long subOf;
    private LovGetListDto subOfDet;
    private String accountType;
    private String controlAcType;
    private String costGroup;
    private Boolean interCompany;
    private Long interCompanyId;
    private LovGetListDto interCompanyDet;
    private String remarks;
    private Integer seqNo;
    private Boolean active;
    private Boolean isKeyFavorite;
    private Boolean billWise;
    private Boolean prepaymentLedger;
    private String createdBy;
    private LocalDateTime createdDate;
    private List<PaymentDetailsDto> paymentDetails;
    private List<CompanyDetailsDto> companyDetails;

    // Additional fields for list API
    private Long parentPoid;     // mirrors subOf in list responses
    private Integer level;       // hierarchy level (0 main groups, 1 direct children)
    private Boolean deleted;     // soft delete flag
    private Long childCount;     // number of direct children
}