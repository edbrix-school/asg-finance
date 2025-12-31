package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostJournalVoucherResponse {
    private Long transactionPoid;
    private String status;
    private String postingReference;
    private String message;
}
