package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SupplierMasterManagementDtlDto {
    private Long supplierPoid;
    private Long detRowId;
    @NotBlank(message = "Name must not be empty")
    @Size(max = 1000, message = "Name must be at most 1000 characters")
    private String name;
    @NotBlank(message = "Designation must not be empty")
    @Size(max = 1000, message = "Designation must be at most 1000 characters")
    private String designation;
    private Long mobile;
    @Size(max = 1000, message = "Email must be at most 1000 characters")
    private String email;
    @Size(max = 1000, message = "Remarks must be at most 1000 characters")
    private String remarks;
    @Size(max = 100, message = "Telephone1 must be at most 100 characters")
    private String telephone1;
    private Long telephone;
    @Size(max = 500, message = "Management mobile must be at most 500 characters")
    private String managementMobile;
    @Size(max = 500, message = "Management telephone must be at most 500 characters")
    private String managementTelephone;
    
    // Action type for management detail operations: isCreated, isUpdated, isDeleted, noChange
    private String actionType;
}
