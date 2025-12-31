package com.asg.finance.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GlFavAcMasterUserRoleDtlDto {

    private Long detRowId;
    private Long favAcPoid;
    private Long userRolePoid;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
}
