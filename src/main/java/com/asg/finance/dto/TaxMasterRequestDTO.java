package com.asg.finance.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxMasterRequestDTO {

    @NotBlank(message = "Tax Code is mandatory")
    @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Tax Code must be alphanumeric and may contain spaces")
    @Size(max = 20, message = "FA Category Code must be at most 20 characters")
    private String taxCode;

    @NotBlank(message = "Tax Name is mandatory")
    @Size(max = 200, message = "FA Category Code must be at most 200 characters")
    private String taxName;

    @Size(max = 200, message = "FA Category Code must be at most 200 characters")
    private String taxName2;

    @DecimalMin(value = "0.0", inclusive = true)
    @DecimalMax(value = "100.0", inclusive = true)
    @Digits(integer = 3, fraction = 2, message = "Percentage can have up to 3 digits and 2 decimals")
    private Double percentage;

    @NotBlank(message = "Tax Type is mandatory")
    @Size(max = 20, message = "FA Category Code must be at most 20 characters")
    private String taxType;

    @NotBlank(message = "GL Type is mandatory")
    private String glType;

    @NotNull(message = "GL Ledger is mandatory")
    private Long glLedgerPoid;

    @NotBlank(message = "Tax Category is mandatory")
    private String taxCategory;

    @Size(max = 1, message = "active must be at most 1 character")
    @Pattern(regexp = "Y|N", message = "Active must be Y or N")
    private String active = "Y";

    @Min(value = 0, message = "Sequence number cannot be negative")
    @Max(value = 99999, message = "Sequence number cannot exceed 5 digits")
    private Integer seqNo;

    private Long groupPoid;
}
