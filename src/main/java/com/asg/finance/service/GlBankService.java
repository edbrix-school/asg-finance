package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.finance.entity.GlBankEntity;
import com.asg.finance.dto.GlBankDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;


public interface GlBankService {
    GlBankDto updateGlBank(Long bankPoid, GlBankDto request);

    GlBankDto createEntry(GlBankDto bankMasterDto);

     GlBankDto fetchGlBank(Long bankPoid);

    void deleteBankMaster(Long bankPoid, DeleteReasonDto deleteReasonDto);

    Map<String, Object> listOfRecordsAndGenericSearch(String docId, FilterRequestDto request, Pageable pageable);
}


