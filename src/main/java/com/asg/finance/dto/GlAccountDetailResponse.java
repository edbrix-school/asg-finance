package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.entity.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Long seqNo;
    private Long company;
    private Company companyDetails;
    private String viewCategory;
    private LovGetListDto viewCategoryDetails;
}

