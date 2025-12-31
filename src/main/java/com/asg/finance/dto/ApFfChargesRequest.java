package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApFfChargesRequest {
    private Long loginGroupPoid;
    private Long loginCompanyPoid;
    private Long loginUserPoid;
    // Semicolon-separated FF TRANSACTION_POIDs
    private String ffPoid;
}
