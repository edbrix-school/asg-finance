package com.asg.finance.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseReallocationXlDetailRequest {

    @NotNull(message = "Company is required")
    private Long company;

    @NotNull(message = "CompanyCode is required")
    private String companyCode;

    private Long detRowId;

    @Size(max = 100, message = "Remarks must not exceed 100 characters")
    private String remarks;

    private String actionType;

    // 🔥 Dynamic fields (SH, FFS, etc.)
    private Map<String, BigDecimal> costCenterMap = new LinkedHashMap<>();

    @JsonAnySetter
    public void setDynamicField(String key, Object value) {
        // Skip known fields
        if (isKnownField(key)) return;

        if (value != null) {
            try {
                costCenterMap.put(key.toUpperCase(), new BigDecimal(value.toString()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid value for " + key);
            }
        }
    }

    private boolean isKnownField(String key) {
        return key.equalsIgnoreCase("company")
                || key.equalsIgnoreCase("companyCode")
                || key.equalsIgnoreCase("detRowId")
                || key.equalsIgnoreCase("remarks")
                || key.equalsIgnoreCase("actionType");
    }
}

