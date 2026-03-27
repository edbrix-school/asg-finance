package com.asg.finance.repository;

import com.asg.finance.dto.ChargeFDADto;
import com.asg.finance.dto.ChargeFFDto;
import com.asg.finance.dto.ItemDetailDto;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public interface BankDebitVoucherCustomRepository {
    void procGlJobValBeforeSave(Long groupPoid, Long userPoid, Long companyPoid, String docId, String refType, String ref);

    void procGlBankPayGlBenVal(Long groupPoid, Long userPoid, Long companyPoid, String docId, Long docKeyPoid, String payingType, String refType, Long payGlPoid, String beneficiaryId, Long bankPoid);

    String[] procGlJobRelOldValues(Long groupPoid, Long userPoid, Long companyPoid, String docId, Long docKeyPoid);

    List<ChargeFFDto> procLoadFFCharges(Long groupPoid, Long userPoid, Long companyPoid, Long ffRefPoid);

    List<ChargeFDADto> procLoadFDACharges(Long groupPoid, Long userPoid, Long companyPoid, Long fdaRefPoid);

    List<ItemDetailDto> procLoadMTAItems(Long groupPoid, Long userPoid, Long companyPoid, Long mtaRefPoid);

    BigDecimal procGetBankBalance(Long groupPoid, Long userPoid, Long companyPoid, String documentId, LocalDate docDate, Long bankPoid);

    String procGetBeneficiaryName(Long groupPoid,Long userPoid, Long companyPoid,String documentId, Long beneficiaryId);

    Long getBankGlPoid(Long bankPoid);

    String procSetMtaRef(Long groupPoid, Long userPoid, Long companyPoid, Long salesQtnRefPoid);
}
