package com.asg.finance.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChequeReturnLoadResponseDto {
    private ChequeReturnDataDto chequeDetails;
    private List<ChequeReturnGlEntryDto> glDetails;
}
