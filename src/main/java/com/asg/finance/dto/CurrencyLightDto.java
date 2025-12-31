package com.asg.finance.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyLightDto {
    private Long currencyPoid;
    private String currencyCode;
    private String currencyName;
}