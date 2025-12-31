package com.asg.finance.dto.masters;

import com.asg.common.lib.dto.UserRoleRightsDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserRoleRightsResponse {

    private String status;
    private String message ;
    private Long roleId;
    private List<UserRoleRightsDto> permissions;
}