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

    private Integer breakupFrom;
    private Integer breakupTo;

    private String actionType;
}
