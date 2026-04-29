package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateAllocationResponse {

    private Boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private Map<String, BigDecimal> totals;

}

