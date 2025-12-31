package com.asg.finance.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GlFavAcMasterGlAcDtlDto {

    private Long detRowId;
    private Long favAcPoid;
    private Long glPoid;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private Long seqNo;
    private Long company;
    private String viewCategory;
}
