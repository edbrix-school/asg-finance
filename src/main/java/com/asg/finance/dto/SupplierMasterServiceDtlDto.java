package com.asg.finance.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierMasterServiceDtlDto {
    private Long supplierPoid;
    private Long detRowId;
    private Long servicePoid;
    private SupplierServicesMasterDto service;
    @Size(max = 1000, message = "Remarks must be at most 1000 characters")
    private String remarks;
    
    // Action type for service detail operations: isCreated, isUpdated, isDeleted, noChange
    private String actionType;
}
