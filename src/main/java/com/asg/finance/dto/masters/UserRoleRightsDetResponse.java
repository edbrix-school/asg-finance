package com.asg.finance.dto.masters;

import com.asg.common.lib.dto.UserRoleRightsDetDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class UserRoleRightsDetResponse {
    private String status;
    private String message;
    private UserRoleRightsDetDto data;
}
