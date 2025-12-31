package com.asg.finance.dto;

import lombok.Data;

@Data
public class CompanyListDto {

    private Long companyPoid;
    private String companyCode;
    private String companyName;
    private String contactPerson;
    private String email;
    private String telephone;
    private String label;
    private Long value;

}