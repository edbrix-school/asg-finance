package com.asg.finance.dto;

import com.asg.common.lib.dto.DetailsDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PropertyCostCenterResponse {

    private Long poid;
    private String propertyCostCenterCode;
    private String propertyCostCenterName;
    private String propertyDescription;
    private String propertyType;
    private Long costCenterPoid;
    private DetailsDto costCenterDet;
    private DetailsDto parentPropertyDet;
    private Long companyPoid;
    private LovGetListDto companyDet;
    private String remarks;
    private Integer seqNo;
    private String deleted;
    private String active;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    
    // Additional fields for tree view support
    private Long parentPropertyPoid;
    private Integer level;
    private String itemType;
}
