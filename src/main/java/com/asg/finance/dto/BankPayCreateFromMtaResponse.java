package com.asg.finance.dto;

import lombok.Data;

import java.util.List;

@Data
public class BankPayCreateFromMtaResponse {
    private String resultMessage;
    private List<BankPayItemDto> items;
}
