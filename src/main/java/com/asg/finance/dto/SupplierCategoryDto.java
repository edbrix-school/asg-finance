package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SupplierCategoryDto {
    private Long supplierCategoryPoid;
    private Long groupPoid;
    //   @NotBlank(message = "supplierCategoryCode is required")
    private String supplierCategoryCode;
    @NotBlank(message = "Supplier Category Name is required")
    private String supplierCategoryName;
    private String supplierCategoryName2;
    private String active;
    private String seqNo;
    private String generalRemarks;
    private String deleted;
    private LocalDateTime createdDate;
    private String createdBy;
}
