package com.asg.finance.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode
public class ArDebitNoteDtlId implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}