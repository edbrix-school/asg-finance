package com.asg.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Credit GL information")
public class CreditGlDto {

    @Schema(description = "GL POID", example = "16427")
    private Long glPoid;

    @Schema(description = "GL Code", example = "GL-1201")
    private String glCode;

    @Schema(description = "GL Description", example = "BANK ACCOUNT - BAHRAIN")
    private String glDescription;
}
