package com.asg.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class BankDepositVoucherRequestDto {
    @NotNull(message = "Bank is mandatory")
    private Long bankPoid;

    @NotNull(message = "Type is mandatory")
    private String type;

    private String bankFilter;

    private Boolean groupPosting;

    private String postingNarration;

    private Long companyPoid;

    private Long groupPoid;

    private String remarks;

    private LocalDate transactionDate;

    @Valid
    private List<BankDepositVoucherDtlDto> details;
}
