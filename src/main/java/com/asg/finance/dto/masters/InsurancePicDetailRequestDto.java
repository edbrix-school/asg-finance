package com.asg.finance.dto.masters;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    
    @NotNull(message = "Role is mandatory")
    private Long rolePoid;
    
    @NotBlank(message = "Contact Type is mandatory")
    private String contactType;
    
    @NotNull(message = "PIC Person is mandatory")
    private Long picPersonPoid;
    
    @NotNull(message = "From Date is mandatory")
    private LocalDate fromDate;
    
    private LocalDate toDate;
    
    private String actionType;
}
