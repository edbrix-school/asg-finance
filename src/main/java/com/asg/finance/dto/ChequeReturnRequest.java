// com/asg/dto/ChequeReturnRequest.java
package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.asg.finance.validation.ValidChequeHeader;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChequeReturnRequest {

    @NotNull
    @Valid
    private ChequeHeaderDto chequeHeader;

    @NotEmpty
    @Valid
    private List<ChequeDetailDto> chequeDetails;

    @NotEmpty
    @Valid
    private List<GlDetailDto> glDetails;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @ValidChequeHeader
    public static class ChequeHeaderDto {
        private Long transactionPoid;   // Auto Generated
        private String docRef;          // Auto Generated Using Trigger
        private LocalDate transactionDate;
        @NotBlank(message = "chequeNumber is mandatory")
        private String chequeNumber;
        @NotBlank(message = "narration/remarks is mandatory")
        private String remarks;
        private String status;
        private String closeDetail;
        private String createdBy;
        private Date createdDate;
        private Date lastModifiedDate;
        private String lastModifiedBy;
        private String receiptNumber;

    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChequeDetailDto {

        private Long detRowId;
        private Long transactionPoid;
        private String actionType;

        @NotNull(message = "amount is mandatory")
        @DecimalMin(value = "0.01", message = "amount must be > 0")
        private Double amount;

        @NotBlank(message = "pymtType is mandatory")
        @Size(max = 10, message = "pymtType cannot exceed 10 characters")
        private String pymtType;

        @NotBlank(message = "chqCardNo is mandatory")
        @Size(max = 50, message = "chqCardNo cannot exceed 50 characters")
        private String chqCardNo;

        @NotNull(message = "chqDate is mandatory")
        @PastOrPresent(message = "chqDate cannot be future-dated")
        private LocalDate chqDate;

        @NotNull(message = "bankPoid is mandatory")
        private Long bankPoid;

        private LovGetListDto bankDtl;

        @NotBlank(message = "chqAcName is mandatory")
        @Size(max = 100, message = "chqAcName cannot exceed 100 characters")
        private String chqAcName;

        @NotBlank(message = "chqAcNo is mandatory")
        @Size(max = 50, message = "chqAcNo cannot exceed 50 characters")
        private String chqAcNo;

        @Size(max = 200, message = "remarks cannot exceed 200 characters")
        private String remarks;

        @NotNull(message = "rcpDate is mandatory")
        private LocalDate rcpDate;
        
        @NotBlank(message = "refDocRef is mandatory")
        @Size(max = 50, message = "refDocRef cannot exceed 50 characters")
        private String refDocRef;

        private Long paymentMainPoid;
        private Long addressPoid;
        private String voucherType;
        private Long choPoid;
        private LocalDate choDate;
        
        @Size(max = 20, message = "refDocId cannot exceed 20 characters")
        private String refDocId;
        
        private Long refDocPoid;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GlDetailDto {
        private Long detRowId;
        private Long transactionPoid;
        private String actionType;

        @NotNull(message = "companyPoid is mandatory")
        private Long companyPoid;

        private LovGetListDto companyDtl;

        @NotBlank(message = "type is mandatory")
        @Pattern(regexp = "^(DR|CR)$", message = "type must be either DR or CR")
        private String type;

        @NotNull(message = "glPoid is mandatory")
        private Long glPoid;

        private LovGetListDto glDtl;

        @NotNull(message = "amount is mandatory")
        @DecimalMin(value = "0.01", message = "amount must be > 0")
        private Double amount; // we’ll map to DR/CR depending on type

        @Size(max = 100, message = "remarks cannot exceed 100 characters")
        private String remarks;
    }
}
