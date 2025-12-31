package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogDto {
    private Long id;
    private String level;
    private String message;
    private String logger;
    private LocalDateTime timestamp;
    private String userId;
    private String action;
}