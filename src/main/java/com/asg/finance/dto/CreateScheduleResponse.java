package com.asg.finance.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateScheduleResponse {
    private String status;
    private String message;
    private List<RecurringJvScheduleDetailResponse> scheduleDetails;
}
