package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApFfChargesRequestDto {
    private Long loginGroupPoid;
    private Long loginCompanyPoid;
    private Long loginUserPoid;
    private String ffPoid;
    private String result;
    private String outdata;


}
