package com.asg.finance.repository;


import com.asg.common.lib.dto.response.GlPostingViewResponseDto;

public interface ImcoDepositRefundRepository {
    GlPostingViewResponseDto fetchGlPostingDetails(
            Long groupPoid,
            Long companyPoid,
            String docId,
            Long transactionPoid
    );

    boolean isValidReceipt(Long receiptPoid, String receiptNumber, Long companyPoid);
}
