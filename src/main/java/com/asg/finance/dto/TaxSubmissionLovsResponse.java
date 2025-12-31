package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxSubmissionLovsResponse {

    private List<Map<String, Object>> company; // COMPANY_FOR_TAX_SUBMISSION
}


