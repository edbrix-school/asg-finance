package com.asg.finance.repository;

import com.asg.finance.dto.PettyCashFromFdaDto;
import com.asg.finance.dto.PettyCashFromFfDto;
import com.asg.finance.dto.PettyCashFromGenrlPoDto;
import com.asg.finance.dto.PettyCashFromGrnDto;
import com.asg.finance.dto.PettyCashFromPoDto;
import com.asg.finance.dto.PettyGlBalanceDto;

import java.util.List;

public interface PettyCashLoadByRefTypeRepository {
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

    List<PettyGlBalanceDto> getPettyGlBalance(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String docId,
            Long docKeyPoid,
            String lovName,
            Long lovValue
    );

    List<PettyCashFromGrnDto> loadPettyCashFromGrn(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String transactionDate,
            String grnSupplierPoid,
            StringBuilder result
    );

    List<PettyCashFromGenrlPoDto> loadPettyCashFromCompletedPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid,
            StringBuilder result
    );

}
