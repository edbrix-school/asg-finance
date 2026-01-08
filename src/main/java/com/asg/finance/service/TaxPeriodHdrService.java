package com.asg.finance.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.TaxPeriodChargeDtlResponseDto;
import com.asg.finance.dto.TaxPeriodHdrRequestDto;
import com.asg.finance.dto.TaxPeriodHdrResponseDto;
import com.asg.finance.dto.TaxPeriodStockDtlResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;


public interface TaxPeriodHdrService {
    TaxPeriodHdrResponseDto createTaxPeriodHdr(TaxPeriodHdrRequestDto request);
    TaxPeriodHdrResponseDto updateTaxPeriodHdr(Long transactionPoid, TaxPeriodHdrRequestDto request);
    TaxPeriodHdrResponseDto getTaxPeriodHdrById(Long transactionPoid);
    void softDeleteTaxPeriodHdr(Long transactionPoid, com.asg.common.lib.dto.DeleteReasonDto deleteReasonDto);
    Page<TaxPeriodChargeDtlResponseDto> getTaxPeriodCharges(Long transactionPoid, Pageable pageable);
    Page<TaxPeriodStockDtlResponseDto> getTaxPeriodStocks(Long transactionPoid, Pageable pageable);
    String copyTaxPeriod(Long transactionPoid, Long companyPoid, Long userPoid);
    Map<String, Object> listTaxPeriod(String documentId, FilterRequestDto filters, Pageable pageable, LocalDate periodFrom, LocalDate periodTo);
}
