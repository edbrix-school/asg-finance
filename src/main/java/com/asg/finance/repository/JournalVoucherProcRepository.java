package com.asg.finance.repository;

import com.asg.finance.dto.JournalVoucherAssetDetailDto;
import com.asg.finance.dto.JournalVoucherCapitalizationDto;
import java.util.List;

public interface JournalVoucherProcRepository {

    List<JournalVoucherAssetDetailDto> fetchAssetDepreciationDetails(Long faPoid);
    
    List<JournalVoucherCapitalizationDto> fetchFixedAssetDetails(Long faPoid);
    
    void updateAssetDetail(Long transactionPoid);
}
