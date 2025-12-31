package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoadVatDetailsResponse {

    private String status; // SUCCESS, ERROR, WARNING
    private String message;
    private List<TaxSubmissionDetailResponse> details;
}


