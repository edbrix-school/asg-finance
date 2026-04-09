package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecurringJVScheduleWiseDetailsDto {

    private LocalDate monthWiseDate;
    private String remarks;
    private Long detRowId;
    private String actionType;

}
