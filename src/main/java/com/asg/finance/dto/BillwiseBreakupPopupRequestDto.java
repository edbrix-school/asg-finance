package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillwiseBreakupPopupRequestDto {
    private Long billDetRowId;
    private String billRefType;
    private String billRef;
    
    private LocalDate billDueDate;
    
    private String type;            // "DR" or "CR"
    private BigDecimal amount;
    private String billRemarks;
    
    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"
}
