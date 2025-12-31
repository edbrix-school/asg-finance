package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitTaxSubmissionResponse {

    private String approvalStatus;
    private String status;
    private String message;
}


