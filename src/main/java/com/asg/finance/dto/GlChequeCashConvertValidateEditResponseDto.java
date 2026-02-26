package com.asg.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for validate-edit: indicates whether the record is eligible for edit (PENDING status)")
public class GlChequeCashConvertValidateEditResponseDto {

    @Schema(description = "True if the record is in PENDING status and allowed to edit", example = "true")
    private boolean valid;

    @Schema(description = "Message describing the result", example = "Record is eligible for edit.")
    private String message;
}
