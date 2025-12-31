package com.asg.finance.dto.masters;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.asg.common.lib.dto.UserRolesDto;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRolesResponse {
    private String status;
    private String message;
    private UserRolesDto data;
    private UserRolesError errors;

    public UserRolesResponse(String status, String message, UserRolesDto data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public UserRolesResponse( String message, String status,UserRolesError errors) {
        this.errors = errors;
        this.message = message;
        this.status = status;
    }


}
