package com.asg.finance.entity.key;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class GlBankDebitChargeDtlId implements Serializable {

    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;
}
