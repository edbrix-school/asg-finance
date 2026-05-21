package com.asg.finance.dto;

import com.asg.common.lib.entity.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


/**
 * Response DTO for GL Account Detail
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlAccountDetailResponse {

    private Long detRowId;
    private Long favAcPoid;
    private Long glPoid;
    private GlMasterDto glDetails;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private Long seqNo;
    private Long company;
    private Company companyDetails;
    private String viewCategory;
}

