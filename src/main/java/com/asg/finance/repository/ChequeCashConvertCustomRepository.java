package com.asg.finance.repository;

public interface ChequeCashConvertCustomRepository {
    String convertChequeAfterSave(
            Long groupPoid,
            Long companyPoid,
            Long transactionPoid,
            String docRef,
            Long loginUserPoid,
            String loginUser
    );
}
