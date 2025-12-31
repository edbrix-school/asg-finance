package com.asg.finance.dto.masters;

import com.asg.common.lib.dto.UserRoleDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleResponse {
    private List<UserRoleDto> roles;
    private long totalCount;
    private int currentPage;
    private int pageSize;
    private int totalPages;
}
