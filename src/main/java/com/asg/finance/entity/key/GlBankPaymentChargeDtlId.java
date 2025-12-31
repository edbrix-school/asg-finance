package com.asg.finance.entity.key;

import lombok.Data;

import java.io.Serializable;

@Data
public class GlBankPaymentChargeDtlId implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}