package com.asg.finance.repository;

import java.util.Map;

public interface ApPaymentRequestCustomRepository {

    Map<String, Object> createFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid
    );

    Map<String, Object> createFromFf(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid
    );

    Map<String, Object> createFromFda(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String fdaPoid
    );

    Map<String, Object> createFromMta(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String poPoid
    );
}
