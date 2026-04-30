package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelexValidationResponseDto {
    private boolean isValid;
    private String message;
    private List<String> errors;
    private List<String> warnings;
    private ValidationDetails validationDetails;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationDetails {
        private boolean hasSelectedRecords;
        private boolean balanceCheckPassed;
        private boolean intermediaryBankDetailsValid;
        private int selectedRecordCount;
        private int totalRecordCount;
        private String balanceCheckResult;
        private List<String> intermediaryBankErrors;
    }
}