package com.asg.finance.dto;

import lombok.Data;

import java.util.List;

@Data
public class BankPayCreateFromFdaResponse {
    private String resultMessage;
    private List<BankPayFdaItemDto> items;
}
