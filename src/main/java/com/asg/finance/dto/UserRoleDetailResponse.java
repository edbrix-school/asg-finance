package com.asg.finance.dto;

import com.asg.common.lib.dto.UserRoleDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private UserRoleDto userRoleDetail;
}

