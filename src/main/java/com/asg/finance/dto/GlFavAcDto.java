package com.asg.finance.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GlFavAcDto {

    private Long favAcPoid;
    private Long groupPoid;
    private String favAcCode;
    private String description;
    private String description2;
    private String active;
    private String createdBy;
    private Long seqNo;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private String deleted;
    private List<GlFavAcMasterGlAcDtlDto> glFavAcMasterGlAcDtlDtoList;
    private List<GlFavAcMasterUserRoleDtlDto> glFavAcMasterUserRoleDtlDtoList;
}
