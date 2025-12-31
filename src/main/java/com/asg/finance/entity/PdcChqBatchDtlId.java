package com.asg.finance.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PdcChqBatchDtlId implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}
