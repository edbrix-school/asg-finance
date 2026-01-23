package com.asg.finance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for GL Account Detail in Favorite Account Master
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlAccountDetailRequest {

    @NotNull(message = "GL Account Poid is mandatory")
    private Long glAccountPoId;

    @NotNull(message = "Company Poid is mandatory")
    private Long companyPoId;

    @NotNull(message = "View Category is mandatory")
    @Size(max = 50, message = "View Category must be at most 50 characters")
    private String viewCategoryPoid;

    @Size(max = 100, message = "Remarks must be at most 100 characters")
    private String remarks;

    private Long seqNo;
    
    private Long detRowId; 
    private String actionType; 
}

