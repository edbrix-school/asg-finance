package com.asg.finance.dto;

import lombok.Data;
import java.util.List;

@Data
public class ImcoRefundLoadResponseDto {
    private List<BillDetailDto> bills;
    private List<ChequeDetailDto> cheques;
    private String payingTo;
    private String blNumber;
}