package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


public interface PettyCashVoucherService {

    PettyCashResponseDto createPettyCash(PettyCashCreateRequestDto requestDto, String documentId);

    PettyCashResponseDto updatePettyCash(Long transactionPoid,
                                         PettyCashUpdateRequestDto requestDto, String documentId);

    PettyCashResponseDto findById(Long transactionPoid, String documentId);



    void deletePettyCashVoucher(Long transactionPoid, String documentId, String refType, DeleteReasonDto deleteReasonDto);

    Map<String, Object> listPettyCashVoucher(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<PettyCashFromPoDto> loadPettyCashFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String rfqPoid,
            StringBuilder result
    );

    List<PettyCashFromFfDto> loadPettyCashFromFf(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid,
            StringBuilder result
    );

    List<PettyCashFromFdaDto> loadPettyCashFromFda(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String fdaPoid,
            StringBuilder result
    );

    List<PettyGlBalanceDto> loadPettyGlBalance(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String documentId,
            Long docKeyPoid,
            String lovName,
            Long lovValue
    );

    byte[] print(Long transactionPoid) throws Exception;

}