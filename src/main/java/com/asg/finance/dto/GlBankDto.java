package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlBankDto {
    private Long bankPoid;
    private Long groupPoid;
    @Size(max = 20, message = "Bank Code must be at most 20 characters")
    @NotBlank(message = "Bank code is required")
    private String bankCode;
    @NotBlank(message = "Bank name is required")
    @Size(max = 100, message = "Bank description must be at most 100 characters")
    private String bankDescription;
    @Size(max = 100, message = "Bank description2 must be at most 100 characters")
    private String bankDescription2;
    @NotNull(message = "Gl poid is required")
    private Long glPoid;
    @NotBlank(message = "Bank account no is required")
    @Size(max = 100, message = "Bank account no must be at most 100 characters")
    private String bankAccountNo;
    @Size(max = 100, message = "Iban must be at most 100 characters")
    private String iban;
    @Size(max = 100, message = "Swift code must be at most 100 characters")
    private String swiftCode;
    @Size(max = 500, message = "Bank address must be at most 500 characters")
    private String bankAddress;
    @Size(max = 20, message = "Currency code must be at most 20 characters")
    private String currencyCode;
    private BigDecimal buyingRate;
    private BigDecimal sellingRate;
    private Date periodStart;
    private Date periodEnd;
    private BigDecimal cardCommision;
    @Size(max = 1, message = "Cheque printing yn must be at most 1 character")
    private String chequePrintingYn;
    @Digits(integer = 5, fraction = 0, message = "Invalid seq no format")
    private Integer seqno;
    @Size(max = 500, message = "Remarks must be at most 500 characters")
    private String remarks;
    @Size(max = 1, message = "Active must be at most 1 character")
    private String active;
    @Size(max = 1, message = "Deleted must be at most 1 character")
    private String deleted;
    private BigDecimal odLimit;
    @NotNull(message = "Company poid is required")
    private Long companyPoid;
    @Size(max = 20, message = "Old gl acc no must be at most 20 characters")
    private String oldGlAccNo;
    @NotBlank(message = "Bank prefix is required")
    @Size(max = 20, message = "Bank prefix must be at most 20 characters")
    private String bankPrefix;
    @Size(max = 1, message = "Online file tt must be at most 1 character")
    private String onlineFileTt;
    private Date bankStatementDate;
    private BigDecimal currencyRate;
    @Size(max = 300, message = "Vat tin number must be at most 300 characters")
    private String vatTinNumber;
    @Size(max = 100, message = "Account type must be at most 100 characters")
    private String accountType;
    @Size(max = 100, message = "Correspondent swift code must be at most 100 characters")
    private String correspondantSwiftCode;
    @Size(max = 500, message = "Correspondent bank must be at most 500 characters")
    private String correspondantBank;
    @Size(max = 300, message = "Edi bank account no must be at most 300 characters")
    private String ediBankAccountNo;
    private String createdBy;
    private Timestamp createdDate;

    private LovGetListDto companyDet;
    private LovGetListDto currencyDet;
    @Valid
    private List<GlBankChequeDtlDto> chequeDetails;
    @Valid
    private List<GlBankCommissionDtlDto> commissionDetails;
}