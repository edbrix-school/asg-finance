package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxSubmissionConfigResponse {

    private Integer vatFilingPeriod; // From VAT_FILING_PERIOD parameter
    private Boolean approvalRequired; // true
    private String reportName; // null (no report mentioned in SRS)
}


