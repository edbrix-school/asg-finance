package com.asg.finance.dto;

import lombok.Data;

import java.util.List;

@Data
public class BankPayCreateFromFdaResponse {
    private List<BankPayFdaItemDto> items;
}
