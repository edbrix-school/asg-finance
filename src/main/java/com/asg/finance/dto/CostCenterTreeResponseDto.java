package com.asg.finance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Cost Center tree response DTO")
public class CostCenterTreeResponseDto {

    @Schema(description = "Cost Center POID", example = "1000")
    private Long costCenterPoid;

    @Schema(description = "Cost Center Code", example = "CC001")
    private String costCenterCode;

    @Schema(description = "Cost Center Description", example = "Administration")
    private String costCenterDescription;

    @Schema(description = "Cost Center Description 2", example = "Admin Dept")
    private String costCenterDescription2;

    @Schema(description = "Cost Center Type", example = "MAIN_GROUP")
    private String costCenterType;

    @Schema(description = "Parent Cost Center POID", example = "100")
    private Long parentCostCenterPoid;

    @Schema(description = "Cost Center Group Type POID", example = "1")
    private Long costCenterGroupTypePoid;

    @Schema(description = "Cost Group Type", example = "ADMIN")
    private String costGroupType;

    @Schema(description = "Company POID", example = "1")
    private Long companyPoid;

    @Schema(description = "Group POID", example = "1")
    private Long groupPoid;

    @Schema(description = "MIS Group", example = "ADMIN")
    private String misGroup;

    @Schema(description = "Remarks", example = "Main admin cost center")
    private String remarks;

    @Schema(description = "Cost Center Child flag", example = "N")
    private String costCenterChild;

    @Schema(description = "Active flag", example = "Y")
    private String active;

    @Schema(description = "Sequence Number", example = "1")
    private Integer seqNo;

    // Tree-specific fields
    @Schema(description = "Unique identifier for the row in UI", example = "row-1000")
    private String id;

    @Schema(description = "Whether the node is expanded in UI", example = "false")
    private Boolean isExpanded;

    @Schema(description = "Whether this is a group row (can have children)", example = "true")
    private Boolean isRowGroup;

    @Schema(description = "List of child cost centers (nested structure)")
    private List<CostCenterTreeResponseDto> children = new ArrayList<>();

    @Schema(description = "Hierarchy level (0-based)", example = "0")
    private Integer level;

    @Schema(description = "Whether the record is deleted", example = "false")
    private Boolean deleted;

    // Audit fields
    @Schema(description = "Created by", example = "ADMIN")
    private String createdBy;

    @Schema(description = "Created date", example = "2023-01-01T00:00:00")
    private LocalDateTime createdDate;

    @Schema(description = "Last modified by", example = "ADMIN")
    private String lastModifiedBy;

    @Schema(description = "Last modified date", example = "2023-01-01T00:00:00")
    private LocalDateTime lastModifiedDate;
}
