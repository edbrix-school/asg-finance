package com.asg.finance.dto;

import lombok.Data;

@Data
public class ChequeStockRequest {
	private String bankCode;
	private String signType;
}
