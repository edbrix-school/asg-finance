package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SupplierMasterPaymentDtlDto {
    private Long supplierPoid;
    private Long detRowId;
    @NotBlank(message = "Bank must not be empty")
    @Size(max = 100, message = "Bank must be at most 100 characters")
    private String bank;
    @Size(max = 50, message = "Swift Code must be at most 50 characters")
    private String swiftCode;
    @Size(max = 50, message = "Account Number must be at most 50 characters")
    private String accountNumber;
    @Size(max = 100, message = "Remarks must be at most 100 characters")
    private String remarks;
    @Size(max = 30, message = "Type must be at most 30 characters")
    private String type;
    @Size(max = 100, message = "Beneficiary Name must be at most 100 characters")
    private String beneficiaryName;
    @Size(max = 250, message = "Address must be at most 250 characters")
    private String address;
    @NotBlank(message = "Bank Address must not be empty")
    @Size(max = 250, message = "Bank Address must be at most 250 characters")
    private String bankAddress;
    @Size(max = 50, message = "Bank Swift Code must be at most 50 characters")
    private String bankSwiftCode;
    @Size(max = 100, message = "Iban must be at most 100 characters")
    private String iban;
    @Size(max = 100, message = "Intermediary Bank must be at most 100 characters")
    private String intermediaryBank;
    @Size(max = 25, message = "Beneficiary Id must be at most 25 characters")
    private String beneficiaryId;
    @Size(max = 50, message = "Intermediary Acct must be at most 50 characters")
    private String intermediaryAcct;
    @Size(max = 50, message = "Intermediary Oth must be at most 50 characters")
    private String intermediaryOth;
    @Size(max = 250, message = "Special Instruction must be at most 250 characters")
    private String specialInstruction;
    private Long intermediaryCountryPoid;
    private Long beneficiaryCountry;
    @Size(max = 1, message = "Active must be at most 1 characters")
    private String active;
    @Size(max = 1, message = "Defaults must be at most 1 characters")
    private String defaults;
    
    // Action type for payment detail operations: isCreated, isUpdated, isDeleted, noChange
    private String actionType;
}


