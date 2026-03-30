package com.asg.finance.repository;

import com.asg.finance.dto.PettyCashFromFdaDto;
import com.asg.finance.dto.PettyCashFromFfDto;
import com.asg.finance.dto.PettyCashFromGenrlPoDto;
import com.asg.finance.dto.PettyCashFromGrnDto;
import com.asg.finance.dto.PettyCashFromPoDto;
import com.asg.finance.dto.PettyGlBalanceDto;
import com.asg.finance.dto.PettyRefTypeResponse;

public interface PettyCashLoadByRefTypeRepository {

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

    PettyRefTypeResponse<PettyGlBalanceDto> getPettyGlBalance(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String docId,
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

}
