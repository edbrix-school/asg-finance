package com.asg.finance.dto;

public record UserLightDto(String USER_ID,
                           Long USER_POID,
                           String USER_NAME,
                           String USER_MOBILE,
                           String USER_EMAIL,
                           String label,
                           Long value) {

    public UserLightDto {
        if (USER_ID != null) {
            USER_ID = USER_ID.toUpperCase();
        }
    }
}

