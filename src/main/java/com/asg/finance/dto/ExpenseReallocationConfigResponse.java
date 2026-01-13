package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseReallocationConfigResponse {

    private Boolean glPosting; // false
    private List<String> allocationColumns; // ["SH", "FF", "FFS", "FFP", "PROPERTIES", "MTA", "PDA", "ADMIN"]
    private Boolean allowEditAfterJvCreation; // false
    private Boolean allowDeleteAfterJvCreation; // false
    private Integer scale; // 3
}

