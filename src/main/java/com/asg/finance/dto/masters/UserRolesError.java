package com.asg.finance.dto.masters;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRolesError {
    private String field;
    private String error;
}
