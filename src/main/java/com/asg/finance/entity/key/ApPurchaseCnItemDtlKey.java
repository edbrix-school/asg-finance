package com.asg.finance.entity.key;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ApPurchaseCnItemDtlKey implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}
