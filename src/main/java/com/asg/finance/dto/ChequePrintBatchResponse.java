package com.asg.finance.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChequePrintBatchResponse {
    private int requestedCount;
    private int printedCount;
    private int skippedCount;
    private List<String> printedTransactions = new ArrayList<>();
    private List<String> messages = new ArrayList<>();
}

