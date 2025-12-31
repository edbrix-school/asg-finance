package com.asg.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillwiseBreakupPopupRequestDto {
    private Long billDetRowId;
    private String billRefType;
    private String billRef;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date billDueDate;
    
    private String type;            // "DR" or "CR"
    private BigDecimal amount;
    private String billRemarks;
    
    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"
}
