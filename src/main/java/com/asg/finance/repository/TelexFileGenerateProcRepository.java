package com.asg.finance.repository;

import com.asg.finance.dto.TelexFileDtlDto;

import java.util.List;
import java.util.Map;

public interface TelexFileGenerateProcRepository {
    List<TelexFileDtlDto> loadTelexTransferData(String bankList);
    String regenerateTelexFile(Long groupPoid, Long companyPoid, Long userPoid, Long docKeyPoid);
    String createBankFileBatchNbb(Long transactionPoid, Long userPoid, Long companyPoid);
    String checkOverdraft(Long transactionPoid);
    String linkBankApproval(Long param1, Long companyPoid, Long userPoid, String docId, String param5, String docKey);
    String getPayingTo(String docRef);
    boolean checkEmployeeIban(String docRef);
    Map<String, String> getBeneficiaryDetails(Long debitTransactionPoid, String docRef);
    String getTtChargeType(Long debitTransactionPoid);
    Map<String, String> getCompanyDetails(Long companyPoid);
    String getCountryCode(Long debitTransactionPoid, String docRef);
    void createBankFilePayment(Long companyPoid, Long transactionPoid, Long userPoid, String companyName, 
                               String address, String country, String docRef, String chargeType, 
                               String countryCode, Long mainTransactionPoid, int seqNo);
    void generateHsbcApiXml(Long companyPoid, Long transactionPoid, Long userPoid, String companyName,
                           String address, String country, String docRef, String chargeType,
                           String countryCode, Long mainTransactionPoid, int seqNo, Long detRowId);
    // AUB-specific methods
    void createBankFilePaymentAub(Long companyPoid, Long transactionPoid, Long userPoid, String companyName,
                                  String address, String country, String docRef, String chargeType,
                                  String countryCode, Long mainTransactionPoid, int seqNo);
    String getCompanyCode(Long companyPoid);
}
