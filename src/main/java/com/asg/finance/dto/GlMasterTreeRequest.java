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
@Schema(description = "Request object for GL Master tree operations")
public class GlMasterTreeRequest {

    @Schema(description = "Include deleted records in the response", example = "false", defaultValue = "false")
    @Builder.Default
    private Boolean includeDeleted = false;

    @Schema(description = "Group POID", example = "1", defaultValue = "1")
    @Builder.Default
    private Long groupPoid = 1L;

    @Schema(description = "Company POID", example = "1", defaultValue = "1")
    @Builder.Default
    private Long companyPoid = 1L;

    @Schema(description = "User POID", example = "100", defaultValue = "100")
    @Builder.Default
    private Long userPoid = 100L;

    @Schema(description = "First filter value (optional)", example = "1001")
    private String filterValue;

}
