package com.asg.finance.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PettyCashUserRoleRequestDto {
    private Long refTypePoid;
    private String refType;
    @Size(max = 100, message = "Description must be at most 100 characters")
    private String description;
    private List<String> userRolePoid;
    private List<String> glPoid;
    @Size(max = 1, message = "Active must be at most 1 character")
    private String active = "Y";
    private Integer seqno;

}
