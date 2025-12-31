package com.asg.finance.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayGLValidationRequest {
    private Long transactionPoid;
    private String payingType;
    private String refType;
    private Long payGlPoid;
    private String payingTo;
    private Long bankPoid;
}

