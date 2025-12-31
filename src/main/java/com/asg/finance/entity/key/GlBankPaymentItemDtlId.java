package com.asg.finance.entity.key;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class GlBankPaymentItemDtlId implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}