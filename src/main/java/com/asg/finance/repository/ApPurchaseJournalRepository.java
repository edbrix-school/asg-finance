package com.asg.finance.repository;

import com.asg.finance.dto.*;

import java.util.List;
import java.util.Map;

public interface ApPurchaseJournalRepository {
    List<ApPurchaseJournalResponseDto> createFromFda(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String fdaPoid,
            StringBuilder result
    );

    List<ApPurchaseJournalResponseDto> createFromFf(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid,
            StringBuilder result
    );

    String updateFfCost(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String ffPoid,
            Long piPoid
    );

    String updateFdaCost(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String fdaPoid,
            Long piPoid
    );

    String validateVoucher(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            String docId,
            String refType,
            String refPoid
    );


    String getSupplierPoidFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid
    );

    String validateBeforeSave(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            String docId,
            String refType,
            String refPoid
    );

    String updateMtaPoBookingDetails(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid,
            Long bookPoid
    );

    String updateGeneralPoStatus(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid,
            Long bookPoid
    );

    List<ApPiFromPoResponseDto> createPiFromPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid,
            StringBuilder resultMsg
    );

    List<ApPiFromGeneralPoResponseDto> createPiFromGeneralPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String poPoid,
            StringBuilder resultMsg
    );

    List<ApPiFaDefaultDetailsDto> getFaDefaultDetails(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String faPoid
    );

    String checkDuplicatePi(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String partyType,
            Long partyPoid,
            String supplierInvNo,
            Long piPoid,
            String billType
    );

    String validateInputVat(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String docId,
            Long docKeyPoid,
            String partyType,
            Long partyPoid,
            Double taxAmount
    );

    Map<String, String> validateGlDetailBeforeSave(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            String docId,
            String refType,
            String glRefPoid,
            String glRefPoid2,
            String glRefPoid3,
            String partyType,
            Long partyPoid
    );

    String checkOutstandingPo(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            Long supplierPoid
    );

    List<ApPurchaseInvRjvDefaultDto> fetchRjvDefaultDetails(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String rjvPoid
    );

    String getSupplierGlPoid(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String partyType,
            Long partyPoid
    );


}
