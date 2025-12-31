package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitTaxSubmissionRequest {

    @NotBlank(message = "Action is required")
    private String action; // "SUBMIT" or "APPROVE"

    private String comments;
}


