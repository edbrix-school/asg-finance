package com.asg.finance.service;

import com.asg.finance.dto.GlChequeCashConvertHdrDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;

import com.asg.finance.dto.GlChequeConversionLoadResponseDto;

public interface GlChequeCashConvertService {

    GlChequeCashConvertHdrDto getGlChequeCashConvert(Long transactionPoid);

    void softDeleteByTransactionPoid(Long transactionPoid, com.asg.common.lib.dto.DeleteReasonDto deleteReasonDto);

    Map<String, Object> listOfRecordsAndGenericSearch(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);

    GlChequeCashConvertHdrDto createGlChequeCashConvert(GlChequeCashConvertHdrDto glChequeCashConvertHdrDto);

    GlChequeCashConvertHdrDto updateGlChequeCashConvert(Long transactionPoid, GlChequeCashConvertHdrDto dto);

    List<GlChequeConversionLoadResponseDto> loadGlChequeConversion(String chequeNumber, String chequeAccNumber, String type);

    byte[] print(Long transactionPoid) throws Exception;
}
