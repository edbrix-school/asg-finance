package com.asg.finance.dto;

import lombok.Data;

import java.util.List;

@Data
public class BankPayCreateFromFfResponse {
    private String resultMessage;
    private List<BankPayFfItemDto> items;
}
