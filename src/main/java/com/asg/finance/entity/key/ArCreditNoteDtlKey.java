package com.asg.finance.entity.key;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ArCreditNoteDtlKey implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}