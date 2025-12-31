package com.asg.finance.dto.masters;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class UserEntity {
    private BigDecimal userPoid;
    private String userId;
    private String userName;
    private String userEmail;
}
