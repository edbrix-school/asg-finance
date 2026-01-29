package com.asg.finance.dto;

import com.asg.finance.config.ThreeDecimalSerializer;
import com.asg.common.lib.dto.DetailsDto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImcoDepositRefundResponseDTO {
    
    private Long poid;
    private LocalDate transactionDate;
    private Long groupPoid;
    private Long companyPoid;
    private String docRef;
    private String remarks;
    @JsonSerialize(using = ThreeDecimalSerializer.class)
    private BigDecimal grandTotal;
    private String blNumber;
    private String receiptNum;
    private DetailsDto receiptNumDtl;
    private String payingTo;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    
    private List<ChequeRefundDetailResponseDTO> chequeRefundDetails;
    private List<ChequeBillDetailResponseDTO> chequeBillDetails;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChequeRefundDetailResponseDTO {
        private Long poid;
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
        @JsonSerialize(using = ThreeDecimalSerializer.class)
        private BigDecimal amount;
        private String status;
        private String remarks;
        private String oldRcpvno;
        private LocalDate rcpDate;
        private String refDocRef;
        private String choDocId;
        private Long paymentMainPoid;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChequeBillDetailResponseDTO {
        private Long poid;
        private Long detRowId;
        private String billRef;
        @JsonSerialize(using = ThreeDecimalSerializer.class)
        private BigDecimal billAmount;
        private String remarks;
    }
}