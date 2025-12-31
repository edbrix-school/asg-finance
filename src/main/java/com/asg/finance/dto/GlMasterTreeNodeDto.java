package com.asg.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tree node DTO for GL Master, extending core response fields")
public class GlMasterTreeNodeDto extends GLMasterResponseDto {

    @Schema(description = "Unique identifier for the row", example = "row-1000")
    private String id;

    @Schema(description = "Whether the node is expanded in UI", example = "false")
    private Boolean isExpanded;

    @Schema(description = "Whether this is a group row", example = "true")
    private Boolean isRowGroup;

    @Schema(description = "Children nodes in the tree")
    @Builder.Default
    private List<GlMasterTreeNodeDto> children = new ArrayList<>();
}


