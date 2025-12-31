package com.asg.finance.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.masters.InsuranceMasterRequestDto;
import com.asg.finance.dto.masters.InsuranceMasterResponseDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface InsuranceMasterService {
    InsuranceMasterResponseDto createInsuranceMaster(InsuranceMasterRequestDto request);
    InsuranceMasterResponseDto updateInsuranceMaster(Long insuranceId, InsuranceMasterRequestDto request);
    void softDeleteInsuranceMaster(Long insuranceId);
    InsuranceMasterResponseDto getInsuranceMasterById(Long insuranceId);
    Map<String, Object> listInsuranceMasters(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);
    InsuranceMasterResponseDto renewInsurance(Long insuranceId, InsuranceMasterRequestDto request);
}