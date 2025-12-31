package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermsAndConditionDto {
    private Long termsPoid;
    private Long detRowId;
    private Long rowSeqNo;
    private String clauseNo;
    private String clauseDetails;
}
