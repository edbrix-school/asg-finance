package com.asg.finance.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankPayeeRequest {
    @NotBlank(message = "Paying Name is mandatory")
    @Size(max = 300, message = "Secondary paying name cannot exceed 300 characters")
    private String payingName;

    @Size(max = 300, message = "Secondary paying name cannot exceed 300 characters")
    private String payingName2;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;

    @Pattern(regexp = "Y|N", message = "Active must be Y or N")
    private String active;

    @Digits(integer = 5, fraction = 0, message = "Sequence number can contain up to 5 whole digits")
    @Positive(message = "Sequence number must be greater than zero")
    private Integer seqNo;

}
