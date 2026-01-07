package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalVoucherResponse {
    private Long transactionPoid;
    private String docRef;
    private String message;
}
