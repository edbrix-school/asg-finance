package com.asg.finance.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface TaxSubmissionService {

    TaxSubmissionResponse createTaxSubmission(CreateTaxSubmissionRequest request);

    TaxSubmissionResponse getTaxSubmissionById(Long transactionPoid);

    TaxSubmissionResponse updateTaxSubmission(Long transactionPoid, UpdateTaxSubmissionRequest request);

    void deleteTaxSubmission(Long transactionPoid);

    Map<String, Object> listTaxSubmission(FilterRequestDto filters,
                                          Pageable pageable, LocalDate periodFrom, LocalDate periodTo);

    LoadVatDetailsResponse loadVatDetails(Long transactionPoid);

    SubmitTaxSubmissionResponse submitTaxSubmission(Long transactionPoid, SubmitTaxSubmissionRequest request);

    ValidatePeriodResponse validatePeriod(ValidatePeriodRequest request);

    /**
     * Runs after-save processing logic (PROC_TAX_SUBMIN_AFTER_SAVE) for a given
     * tax submission and returns the updated header/details.
     */
    TaxSubmissionResponse runAfterSave(Long transactionPoid);
}


