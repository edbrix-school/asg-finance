package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.*;
import com.asg.finance.dto.PettyRefTypeResponse;
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

    PettyRefTypeResponse<PettyCashFromPoDto> loadPettyCashFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String rfqPoid
    );

    PettyRefTypeResponse<PettyCashFromFfDto> loadPettyCashFromFf(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid
    );

    PettyRefTypeResponse<PettyCashFromFdaDto> loadPettyCashFromFda(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String fdaPoid
    );

    PettyRefTypeResponse<PettyGlBalanceDto> loadPettyGlBalance(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String documentId,
            Long docKeyPoid,
            String lovName,
            Long lovValue
    );

    PettyRefTypeResponse<PettyCashFromGrnDto> loadPettyCashFromGrn(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String transactionDate,
            String grnSupplierPoid
    );

    PettyRefTypeResponse<PettyCashFromGenrlPoDto> loadPettyCashFromCompletedPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid
    );

    List<String> getAllowedRefTypes(Long userPoid);

    PettyCashGlobalParamsDto getPettyCashGlobalParams(Long pettyCashGlPoid);

    byte[] print(Long transactionPoid) throws Exception;

}