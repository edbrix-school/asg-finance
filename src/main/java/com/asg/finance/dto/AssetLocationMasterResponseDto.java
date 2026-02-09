package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetLocationMasterResponseDto {
    private Long locationPoid;
    private String locationCode;
    private String description;
    private Integer seqNo;
    private Long groupPoid;
    private String createdBy;
    private java.time.LocalDateTime createdDate;
    private String lastModifiedBy;
    private java.time.LocalDateTime lastModifiedDate;
    private String deleted;
    private String active;
}
