package com.asg.finance.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlMasterDto {

    private Long glPoid;
    private Long groupPoid;
    private String glCode;
    private String glDescription;
    private String glDescription2;
    private String glType;
    private Long groupGlPoid;
    private String controlAcNature;
    private String costGroup;
    private String billwise;
    private String prepaymentLedger;
    private String interCompanyAc;
    private Long interCompanyPoid;
    private String remarks;
    private Integer seqno;
    private String active;
    private String createdBy;
    private LocalDateTime createdDate;
    private String groupCodeOld;
    private String glAcType;
    private String deleted;
    private Double amountLimit;
    private Double amountRol;
    private String oldOrgCode;
    private String oldOriginalCode;
    private String oldModCode;
}

