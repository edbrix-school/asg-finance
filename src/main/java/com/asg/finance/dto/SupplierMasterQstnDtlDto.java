package com.asg.finance.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierMasterQstnDtlDto {
    private Long supplierPoid;
    private Long detRowId;
    @Size(max = 1000, message = "Questionaries must be at most 1000 characters")
    private String questionaries;
    @Size(max = 1000, message = "Answers must be at most 1000 characters")
    private String answers;
    @Size(max = 1000, message = "Remarks must be at most 1000 characters")
    private String remarks;
    
    // Action type for questionaries detail operations: isCreated, isUpdated, isDeleted, noChange
    private String actionType;
}
