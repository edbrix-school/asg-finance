package com.asg.finance.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdvancePettyCashDtlId implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}