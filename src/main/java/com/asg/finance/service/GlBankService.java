package com.asg.finance.service;

import com.asg.finance.entity.GlBankEntity;
import com.asg.finance.dto.GlBankDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;


public interface GlBankService {
    GlBankDto updateGlBank(Long bankPoid, GlBankDto request);

    GlBankEntity createEntry(GlBankDto bankMasterDto);

     GlBankDto fetchGlBank(Long bankPoid);

    void deleteBankMaster(Long bankPoid, com.asg.common.lib.dto.DeleteReasonDto deleteReasonDto);

    Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, Pageable pageable);
}


