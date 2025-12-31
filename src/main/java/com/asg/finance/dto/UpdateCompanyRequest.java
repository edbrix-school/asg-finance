package com.asg.finance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class UpdateCompanyRequest {
    @NotNull(message = "Company POID is required for update")
    private Long companyPoid;
    
    @NotBlank(message = "Company code is required")
    private String companyCode;
    
    @NotBlank(message = "Company name is required")
    private String companyName;
    
    private String companyName2;
    private String contactPerson;
    private String telephone;
    private String fax;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String countryId;
    private String stateId;
    private String companyColor;
    private String address;
    
    @NotNull(message = "Financial period start is required")
    private Date financialPeriodStart;
    
    @NotNull(message = "Financial period end is required")
    private Date financialPeriodEnd;
    
    @NotNull(message = "Report period start is required")
    private Date reportPeriodStart;
    
    @NotNull(message = "Report period end is required")
    private Date reportPeriodEnd;
    
    private Date transPeriodStart;
    private Date transPeriodEnd;
    private Date provisionalClosedDate;
    private Date stockPeriodStart;
    private Date stockPeriodEnd;
    private Date vatRegistrationDate;
    private Date vatLastFiledDate;
    
    private String active;
    private Integer seqNo;
    private String bankDetail;
    private Long bankPoid;
    private String tinNumber;
    private String accountPerson;
    private String vatFilingPeriod;
    private String accountEmail;
    private String logoImageBase64;
    private String dateFormat;
    private Long timezoneId;
    private Long currencyPoid;
    private Long submissionPeriod;
    
    private List<CompanyDivisionRequestDto> divisions;
}
