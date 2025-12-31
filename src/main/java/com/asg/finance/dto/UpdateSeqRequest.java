package com.asg.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to update sequence number for reordering")
public class UpdateSeqRequest {
    private String fileNameMapped;   // Mapped file name (ASG_xxx.pdf)
    private Long newSeqNo;           // New sequence number for reordering
}