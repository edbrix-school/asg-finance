package com.asg.finance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Property Cost Center tree view operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for GL Master tree operations")
public class PropertyCostCenterTreeRequest {

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

    @Schema(description = "Filter value for searching property cost center description or code", example = "Office")
    private String filterValue;
}

