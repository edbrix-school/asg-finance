package com.asg.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for User Role Detail in Favorite Account Master
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleDetailRequest {

    private Long userRolePoid;

    private Long detRowId; 
    private String actionType; 
}

