package com.asg.finance.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApPaymentRequestResponse {
    private String message;
    private List<Map<String, Object>> records;
}
