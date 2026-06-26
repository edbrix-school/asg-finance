package com.asg.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private BigDecimal billOriginalAmount;
    private String billRemarks;
    private Long glCompanyPoid;
    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"
}
