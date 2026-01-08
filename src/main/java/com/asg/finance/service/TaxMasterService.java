package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.TaxMasterDto;
import com.asg.finance.dto.TaxMasterRequestDTO;
import com.asg.finance.dto.TaxMasterResponseDTO;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface TaxMasterService {
    TaxMasterResponseDTO createTaxMaster(TaxMasterRequestDTO request);
    TaxMasterResponseDTO updateTaxMaster(Long taxPoid, TaxMasterRequestDTO request);
    TaxMasterResponseDTO getTaxMasterById(Long taxPoid);
    void softDeleteTaxMaster(Long taxPoid, DeleteReasonDto deleteReasonDto);
    Map<String, Object> listTaxMaster(String documentId, FilterRequestDto filters, Pageable pageable);
    TaxMasterDto getTaxMasterDtoById(Long taxPoid);
    List<TaxMasterDto> getTaxMasterDtosByIds(List<Long> taxPoids);
}
