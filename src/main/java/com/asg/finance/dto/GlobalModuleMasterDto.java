package com.asg.finance.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalModuleMasterDto {
    private String moduleId;
    private String moduleName;
    private String moduleName2;
    private String groupCode;
    private String moduleShortName;
    private String active;
    private Integer seqNo;
    private Long modulePoid;
    private String deleted;
    private String division;
}
