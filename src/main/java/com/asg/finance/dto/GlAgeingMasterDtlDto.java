package com.asg.finance.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlAgeingMasterDtlDto {

    private Long detRowId;

    @NotBlank(message = "Breakup Title is required")
    @Size(max = 50, message = "Breakup Title must be at most 50 characters")
    private String breakupTitle;

    @NotNull(message = "Breakup From is required")
    @Min(value = 0, message = "Breakup From must be 0 or greater")
    private Integer breakupFrom;

    @NotNull(message = "Breakup To is required")
    @Min(value = 0, message = "Breakup To must be 0 or greater")
    private Integer breakupTo;

    private String actionType;
}
