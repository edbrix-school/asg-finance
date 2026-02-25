package com.asg.finance.dto;

import com.asg.common.lib.dto.UserRoleDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for User Role Detail
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleDetailResponse {

    private Long detRowId;
    private Long favAcPoid;
    private Long userRolePoid;
    private String remarks;
    private UserRoleDto userRoleDetail;
}

