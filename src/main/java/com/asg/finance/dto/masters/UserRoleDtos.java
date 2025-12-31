package com.asg.finance.dto.masters;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserRoleDtos {
    private Long userRoleId;
    private String userRoleName;
    private String active;

    public UserRoleDtos(Long userRoleId, String userRoleName, String active) {
        this.userRoleId = userRoleId;
        this.userRoleName = userRoleName;
        this.active = active;
    }
}
