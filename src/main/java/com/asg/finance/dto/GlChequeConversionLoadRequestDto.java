package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GlChequeConversionLoadRequestDto {


    private String chequeNum;

    private String chqAcNo;
    @NotBlank(message = "Type is required")
    private String type;

}


