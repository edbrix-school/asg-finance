package com.asg.finance.repository;

import com.asg.finance.dto.BankPayCreateFromFdaResponse;
import com.asg.finance.dto.BankPayCreateFromFfResponse;
import com.asg.finance.dto.BankPayCreateFromMtaResponse;

public interface BankPaymentLoadDataRepository {
    
    //BankPaymentChargeDetailResponse loadFfCharges(Long ffRefId);

    BankPayCreateFromFfResponse executeBankPayFromFf(String ffPoid);

    BankPayCreateFromFdaResponse executeBankPayFromFda(String fdaPoid);
    
    //List<BankPaymentChargeDetailResponse> loadFdaCharges(Long fdaRefId);
    
    //List<BankPaymentItemDetailResponse> loadMtaItems(Long mtaRfqId);

    BankPayCreateFromMtaResponse executeBankPayProc(String rfqPoid);
}
