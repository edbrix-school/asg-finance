package com.asg.finance.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlBankChequeDtlDto {
    private Long DetRowId;
    @Size(max = 20, message = "Chq Sign Type must be at most 20 characters")
    private String chqSignType;
    private BigDecimal totalCheques;
    @Size(max = 50, message = "Start Chq No must be at most 50 characters")
    private String startChqNo;
    @Size(max = 50, message = "End Chq No must be at most 50 characters")
    private String endChqNo;
    private BigDecimal reorderLevel;
    @Size(max = 50, message = "Current Chq No must be at most 50 characters")
    private String currentChqNo;
    @Size(max = 200, message = "Default Printer Address must be at most 200 characters")
    private String defaultPrinterAddr;
    @Size(max = 200, message = "Default Printer Tray must be at most 200 characters")
    private String defaultPrinterTray;
    @Size(max = 1, message = "Stock Finished Yn must be at most 1 character")
    private String stockFinishedYn;
    @Size(max = 100, message = "Remarks must be at most 100 characters")
    private String remarks;
    @Size(max = 50, message = "Last Chq No must be at most 50 characters")
    private String lastChqNo;
    private String actionType;
}
