package com.asg.finance.repository;

import com.asg.finance.dto.ImcoRefundLoadResponseDto;

import java.sql.SQLException;

public interface ImcoChequeDetailsRepository {

    ImcoRefundLoadResponseDto fetchChequeAndBillDetails(
            Long groupPoid,
            Long companyPoid,
            String loginUser,
            Long receiptPoid,
            String receiptNumber
    ) throws SQLException;
}
