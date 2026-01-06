package com.asg.finance.service;

import com.asg.common.lib.dto.response.GlPostingViewResponseDto;
import com.asg.finance.dto.BankDepositVoucherDtlDto;
import com.asg.finance.dto.BankDepositVoucherRequestDto;
import com.asg.finance.dto.BankDepositVoucherResponseDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface BankDepositVoucherService {
    BankDepositVoucherResponseDto createBankDepositVoucher(BankDepositVoucherRequestDto request);
    BankDepositVoucherResponseDto updateBankDepositVoucher(Long transactionPoid, BankDepositVoucherRequestDto request);
    BankDepositVoucherResponseDto getBankDepositVoucherById(Long transactionPoid);
    void softDeleteBankDepositVoucher(Long transactionPoid);
    Map<String, Object> listBankDepositVouchers(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);
    List<BankDepositVoucherDtlDto> loadPendingPayments(Long bankPoid, String type, String bankFilter);
    byte[] print(Long transactionPoid) throws Exception;
}
