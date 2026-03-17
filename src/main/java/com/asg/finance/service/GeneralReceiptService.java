package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.finance.dto.GeneralReceiptRequest;
import com.asg.finance.dto.GeneralReceiptResponse;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface GeneralReceiptService {

    /**
     * Create a new general receipt
     * @param request Receipt request with header, payments, bills, and charges
     * @return Receipt response with generated receipt number
     */
    GeneralReceiptResponse createGeneralReceipt(GeneralReceiptRequest request);

    /**
     * Get general receipt by transaction POID
     * @param transactionPoid Transaction POID
     * @return Receipt response with enriched data from procedures
     */
    GeneralReceiptResponse getGeneralReceiptByTransactionPoid(Long transactionPoid);

    /**
     * Get general receipt by document reference (receipt number)
     * @param docRef Document reference (receipt number), e.g., "ASGGEN72675"
     * @return Receipt response with enriched data from procedures
     */
    GeneralReceiptResponse getGeneralReceiptByDocRef(String docRef);

    /**
     * Update existing general receipt
     * @param transactionPoid Transaction POID
     * @param request Updated receipt request
     * @return Updated receipt response
     */
    GeneralReceiptResponse updateGeneralReceipt(Long transactionPoid, GeneralReceiptRequest request);

    /**
     * Delete general receipt (soft delete)
     * @param transactionPoid Transaction POID
     */
    void deleteGeneralReceipt(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    /**
     * Generic list with search and pagination using DocumentService
     */
    Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Get GL account for charge type
     * @param chargeType Charge type (BANK_CHARGES, ROUND_OFF, EXCHANGE_GAIN_LOSS, COURIER_CHARGES)
     * @return GL POID
     */
    String getChargeGLAccount(String chargeType);

    /**
     * Get pending bills for GL account
     * @param glPoid GL POID
     * @param asOnDate As on date
     * @return List of pending bills
     */
    Map<String, Object> getPendingBills(Long glPoid, java.time.LocalDate asOnDate);

    /**
     * Check if a GL account is billwise enabled
     * Calls PROC_AR_GL_BILWISE_YN and returns billwise flag (Y/N)
     *
     * @param glPoid GL POID
     * @return Billwise flag (Y/N)
     */
    String getGlBillwiseYn(Long glPoid);

    /**
     * Check if a GL account requires a cost center
     * Calls PROC_AR_GL_COSTCENTER_YN and returns flag (Y/N/NILL)
     *
     * @param glPoid GL POID
     * @return Cost center flag (Y/N/NILL)
     */
    String getGlCostCenterYn(Long glPoid);

    /**
     * @param transactionPoid
     * @return
     * @throws Exception
     */
    byte[] print(Long transactionPoid) throws Exception;
}

