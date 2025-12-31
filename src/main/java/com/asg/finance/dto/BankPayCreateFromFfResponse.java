package com.asg.finance.dto;

import lombok.Data;

import java.util.List;

@Data
public class BankPayCreateFromFfResponse {
    private List<BankPayFfItemDto> items;
}
