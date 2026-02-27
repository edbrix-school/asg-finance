package com.asg.finance.dto.masters;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsurancePicDetailRequestDto {
    
    private Long detRowId;
    private Long rolePoid;
    private String contactType;
    private Long picPersonPoid;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String actionType;
    
    @JsonIgnore
    public boolean isDeleted() {
        return "isDeleted".equalsIgnoreCase(actionType);
    }
}
