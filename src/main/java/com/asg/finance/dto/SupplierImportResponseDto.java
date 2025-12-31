package com.asg.finance.dto;

import lombok.Data;
import java.util.List;

@Data
public class SupplierImportResponseDto {
    private String status;
    private Integer processedRows;
    private Integer failedRows;
    private List<RowError> errors;

    @Data
    public static class RowError {
        private Integer row;
        private String error;
    }
}