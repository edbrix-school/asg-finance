package com.asg.finance.dto;


import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChequeReturnResponse {
    private ChequeReturnRequest.ChequeHeaderDto chequeHeader;
    private List<ChequeReturnRequest.ChequeDetailDto> chequeDetails;
    private List<ChequeReturnRequest.GlDetailDto> glDetails;
}
