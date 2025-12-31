package com.asg.finance.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SupplierMasterQstnDto {


    private Long supplierPoid;
    private Long detRowId;
    private String qstn;
    private String answers;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
