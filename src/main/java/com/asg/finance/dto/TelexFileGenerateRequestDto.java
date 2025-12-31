package com.asg.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelexFileGenerateRequestDto {
    private Long bankPoid;
    
    @Size(max = 1, message = "Bank list must be maximum 1 character")
    private String bankList;
    
    private LocalDate transactionDate;
    private String remarks;
    private boolean approvalOnly;
    private boolean suppressBalanceCheck;

    @Valid
    private List<TelexFileDtlDto> details;
}
