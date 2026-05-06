package com.asg.finance.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseReallocationDetailResponseDto {

    private Long transactionPoid;
    private Long detRowId;
    private Long company;
    private String companyCode;
    private String companyName;

    @JsonIgnore
    private Map<String, BigDecimal> costCenterMap = new HashMap<>();

    @JsonAnyGetter
    public Map<String, BigDecimal> getDynamicFields() {
        return costCenterMap;
    }
}
