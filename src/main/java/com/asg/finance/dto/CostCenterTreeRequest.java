package com.asg.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for Cost Center tree operations")
public class CostCenterTreeRequest {

    @Schema(description = "Include soft-deleted records", example = "false")
    @Builder.Default
    private Boolean includeDeleted = false;

    @Schema(description = "Group POID for filtering", example = "1", defaultValue = "1")
    @Builder.Default
    private Long groupPoid = 1L;

    @Schema(description = "Company POID for filtering", example = "1", defaultValue = "1")
    @Builder.Default
    private Long companyPoid = 1L;

    @Schema(description = "User POID for filtering", example = "100", defaultValue = "100")
    @Builder.Default
    private Long userPoid = 100L;

    @Schema(description = "Filter value for searching cost center description or code", example = "Admin")
    private String filterValue;
}
