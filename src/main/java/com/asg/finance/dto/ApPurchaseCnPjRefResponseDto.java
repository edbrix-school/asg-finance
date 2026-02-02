package com.asg.finance.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ApPurchaseCnPjRefResponseDto {
    private String pjRefType; // MTA, FF, FDA, GENERAL_PO
    private String pjRefDetails;
    private List<Map<String, Object>> lineItems;
}
