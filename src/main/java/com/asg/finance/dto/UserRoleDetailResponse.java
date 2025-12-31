package com.asg.finance.dto;

import com.asg.common.lib.dto.UserRoleDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

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
    private Timestamp createdDate;
    private String lastModifiedBy;
    private Timestamp lastModifiedDate;
    private UserRoleDto userRoleDetail;
}

