package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface BankDebitVoucherService {

    BankDebitVoucherResponse createBankDebitVoucher(BankDebitVoucherRequest request, String documentId);

    BankDebitVoucherResponse getBankDebitVoucher(Long transactionPoid, String documentId);

    BankDebitVoucherResponse updateBankDebitVoucher(Long transactionPoid, BankDebitVoucherRequest request, String documentId);

    void softDeleteBankDebitVoucher(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    Map<String, Object> listBankDebitVouchers(String documentId, FilterRequestDto filters, LocalDate startDateValue, LocalDate endDateValue, Pageable pageable);

    List<ChargeFFDto> loadFFCharges(Long ffRefPoid);

    List<ChargeFDADto> loadFDACharges(Long fdaRefPoid);

    // Utilities
    BigDecimal getBankBalance(Long bankPoid,String documentId, Date docDate);

    String getBeneficiaryName(Long beneficiaryId,String documentId);

    void validatePayGLAndBeneficiary(PayGLValidationRequest request);

    @Transactional
    void revertReconciliation(Long transactionPoid, String comments);

    byte[] print(Long transactionPoid) throws Exception;
}
