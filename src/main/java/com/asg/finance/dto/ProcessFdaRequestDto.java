package com.asg.finance.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProcessFdaRequestDto {
    private Long fdaPoid;
    private LocalDate transactionDate;
    private Long partyPoid;
    private String partyType;
}
