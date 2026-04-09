package com.asg.finance.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApPaymentRequestDetailResponse {
    private List<ApPaymentRequestDtlResponseDto> details;
    private List<ApPaymentRequestStockDtlResponse> stockDetails;
}
