package com.asg.finance.repository;

import com.asg.finance.dto.JournalVoucherAssetDetailDto;
import com.asg.finance.dto.JournalVoucherCapitalizationDto;
import com.asg.finance.entity.GlJournalVoucherHdr;

import java.sql.SQLException;
import java.util.List;

public interface JournalVoucherProcRepository {

    String postJournalVoucher(Long transactionPoid, GlJournalVoucherHdr glJournalVoucherHdr,String documentId);
    
    List<JournalVoucherAssetDetailDto> fetchAssetDepreciationDetails(Long faPoid);
    
    List<JournalVoucherCapitalizationDto> fetchFixedAssetDetails(Long faPoid) throws SQLException;
    
    void updateAssetDetail(Long transactionPoid);
}
