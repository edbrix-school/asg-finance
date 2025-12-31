package com.asg.finance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaxSubmissionRequest {

    private Long companyId; // Optional, defaults to session company (read-only)

    @NotNull(message = "Period From is required")
    private Timestamp periodFrom;

    @NotNull(message = "Period To is required")
    private Timestamp periodTo;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String remarks;

    private String docRef; // Optional, auto-generated
}


