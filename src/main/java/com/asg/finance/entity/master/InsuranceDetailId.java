package com.asg.finance.entity.master;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceDetailId implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}