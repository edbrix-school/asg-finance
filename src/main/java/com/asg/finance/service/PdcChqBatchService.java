package com.asg.finance.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

public interface PdcChqBatchService {

    PdcChqBatchHdrResponseDto createBatch(PdcChqBatchHdrRequestDto dto);

    PdcChqBatchHdrResponseDto updateBatch(Long transactionPoid, PdcChqBatchHdrRequestDto dto);

    PdcChqBatchHdrResponseDto findById(Long transactionPoid);

    void deletePdcBatch(Long transactionPoid);

    Map<String, Object> listPdcBatchCreation(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);

    PayGlBreakupCheckResponseDto validatePayGl(Long payGlPoid);

    PdcBatchCreationProcResponse processBatch(PdcBatchCreationProcRequest request);

    PdcBatchCreationProcResponse runBankPostingProcedure(PdcBankPostingProcRequest request);

    PdcBatchCreationProcResponse createBatchFromExcel(PdcBatchCreationExcelProcRequest request);

    String uploadExcel(MultipartFile file) throws Exception;


}
