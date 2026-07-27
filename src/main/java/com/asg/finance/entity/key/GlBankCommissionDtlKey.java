package com.asg.finance.entity.key;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class GlBankCommissionDtlKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long detRowId;
    private Long bankPoid;
}
