package com.asg.finance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree node DTO for Property Cost Center tree view
 * Extends the base response DTO and adds tree-specific fields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropertyCostCenterTreeNodeDto extends PropertyCostCenterResponse {
    
    /**
     * Unique identifier for tree node (same as poid, used for tree display)
     */
    private String id;
    
    /**
     * Whether this node is expanded in the tree view
     */
    private Boolean isExpanded;
    
    /**
     * Whether this is a row group (GROUP type)
     */
    private Boolean isRowGroup;
    
    /**
     * Child nodes in the tree hierarchy
     */
    @Builder.Default
    private List<PropertyCostCenterTreeNodeDto> children = new ArrayList<>();
}

