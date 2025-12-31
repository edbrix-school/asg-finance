package com.asg.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlAgeingMasterDto {

    private Long ageingPoid;

    @NotNull(message = "Group Poid is required")
    private Long groupPoid;

    @NotBlank(message = "Description is required")
    @Size(max = 100, message = "Description must be at most 100 characters")
    private String description;

    @Size(max = 100, message = "Description 2 must be at most 100 characters")
    private String description2;

    @Size(max = 20, message = "Ageing Breakup Type must be at most 20 characters")
    private String ageingBreakupType;

    private Integer seqno;

    @NotNull(message = "Active is required")
    private Boolean active;

    private String deleted;
    private String createdBy;
    private Date createdDate;
    private String lastModifiedBy;
    private Date lastModifiedDate;
    @Valid
    @NotEmpty(message = "Ageing Details cannot be empty")
    private List<GlAgeingMasterDtlDto> ageingDetails;
}
