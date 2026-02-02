package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.ReconcileResultDto;
import com.asg.finance.dto.*;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Map;

public interface BankPaymentVoucherService {

    BankPaymentVoucherResponse getVoucherById(Long transactionPoid, String documentId);

    BankPaymentVoucherResponse createBankPaymentVoucher(BankPaymentVoucherRequest req, String documentId);

    BankPaymentVoucherResponse updateBankPaymentVoucher(Long transactionPoid, BankPaymentVoucherRequest req, String documentId);

    void softDeleteVoucher(Long transactionPoid, String documentId, DeleteReasonDto deleteReasonDto);

    Map<String, BigDecimal> getBankBalance(String docId, Long docKeyPoid, Date docDate, Long bankPoid);

    BankPayCreateFromFfResponse createBankPayFromFf(String ffPoid);

    BankPayCreateFromFdaResponse createBankPayFromFda(String fdaPoid);

    BankPayCreateFromMtaResponse createBankPayment(String rfqPoid);

    void validateChequePrint(Long transactionPoid);

    void markChequePrinted(Long transactionPoid);

    void releaseCheque(Long transactionPoid, String releasedTo, String contact);

    void unReleaseCheque(Long transactionPoid);

    void resetChequeStatus(Long transactionPoid);

    String revertReconciliation(Long transactionPoid, String documentId);

    ReconcileResultDto getReconciledDate(String documentId, Long transactionPoid);

    Map<String, Object> listBankPaymentVouchers(String documentId, FilterRequestDto filters, java.time.LocalDate startDateValue, java.time.LocalDate endDateValue, Pageable pageable);

    byte[] print(Long transactionPoid) throws Exception;

}
