package com.asg.finance.entity.key;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlBankDebitItemDtlId implements Serializable {

    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;
}
