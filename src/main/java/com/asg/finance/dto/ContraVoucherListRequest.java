package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContraVoucherListRequest {

    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private String docRef;
    private String narration;
    private String deleted;
    private Long companyId;
    private String currencyCode;
    private Long creditGl;
    private Long debitGl;
}

