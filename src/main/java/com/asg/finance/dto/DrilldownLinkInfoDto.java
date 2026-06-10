package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrilldownLinkInfoDto {
    private Long companyPoid;
    private String targetDocId;
    private Long docKeyPoid;
    private String docName;
}
