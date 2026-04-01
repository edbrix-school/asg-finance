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
    // 🔥 MUST ADD THIS
    private List<PdcChqBatchDtlResponseDto> chequeDetails;
}
