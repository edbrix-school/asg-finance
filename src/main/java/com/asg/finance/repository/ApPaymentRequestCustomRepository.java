package com.asg.finance.repository;

import com.asg.finance.dto.ApPaymentRequestResponse;

import java.util.Map;

public interface ApPaymentRequestCustomRepository {

    ApPaymentRequestResponse createFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid
    );

    ApPaymentRequestResponse createFromFf(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid
    );

    ApPaymentRequestResponse createFromFda(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String fdaPoid
    );

    ApPaymentRequestResponse createFromMta(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String mtaPoid
    );
}
