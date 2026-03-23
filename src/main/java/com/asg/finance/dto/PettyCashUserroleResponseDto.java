package com.asg.finance.dto;

import com.asg.common.lib.dto.DetailsDto;
import com.asg.common.lib.dto.UserRoleDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PettyCashUserroleResponseDto {
    private Long refTypePoid;
    private String refType;
    private String description;
    private List<String> userRolePoid;
    private List<String> glPoid;
     private List<DetailsDto> pettyCashGlPoidDet;
    private List<UserRoleDto> userRolesPoidDet;
    private String active ;
    private Integer seqno;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

}
