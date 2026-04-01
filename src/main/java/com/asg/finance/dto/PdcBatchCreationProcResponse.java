package com.asg.finance.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdcBatchCreationProcResponse {
    private String status;

    private List<PdcChqBatchDtlResponseDto> chequeDetails;
}
