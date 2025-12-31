package com.asg.finance.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChequeReturnEditRequest {

    // --- Header fields ---
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(OPEN|CLOSED)$", message = "Status must be either OPEN or CLOSED")
    private String status; // e.g. OPEN, CLOSED

    @NotBlank(message = "closeDetail is required")
    private String closeDetail;

}
