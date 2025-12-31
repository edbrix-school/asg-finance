package com.asg.finance.dto.masters;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClauseRequestDto {

    private Long detRowId;

    @Min(value = 1, message = "Row sequence must be >= 1")
    private Long rowSeq;

    private String clauseNo;

    @NotBlank(message = "Clause details are required")
    private String clauseDetails;

    private String active;
}