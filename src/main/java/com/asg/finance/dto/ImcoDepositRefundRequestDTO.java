package com.asg.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImcoDepositRefundRequestDTO {
    
    @NotNull(message = "Transaction date is mandatory")
    private LocalDate transactionDate;

    private String docRef;
    private String remarks;
    private BigDecimal grandTotal;
    private String blNumber;
    
    @NotBlank(message = "Receipt number is mandatory")
    private String receiptNum;

    private String payingTo;
    
    @Valid
    @NotEmpty(message = "Cheque refund details cannot be empty")
    private List<ChequeRefundDetailDTO> chequeRefundDetails;
    
    @Valid
    @NotEmpty(message = "Cheque bill details cannot be empty")
    private List<ChequeBillDetailDTO> chequeBillDetails;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChequeRefundDetailDTO {
        private Long detRowId;
        private Long choPoid;
        private LocalDate choDate;
        private String refDocId;
        private Long refDocPoid;
        private String pymtType;
        private String chqCardno;
        private LocalDate chqDate;
        private Long bankPoid;
        private Long addressPoid;
        private String chqAcName;
        private String chqAcNo;
        private BigDecimal amount;
        private String status;
        private String remarks;
        private String oldRcpvno;
        private LocalDate rcpDate;
        private String refDocRef;
        private String choDocId;
        private Long paymentMainPoid;
        private String action; // CREATE, EDIT, DELETE

    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChequeBillDetailDTO {
        private Long detRowId;
        private String billRef;
        private BigDecimal billAmount;
        private String remarks;
        private String action; // CREATE, EDIT, DELETE

    }
}