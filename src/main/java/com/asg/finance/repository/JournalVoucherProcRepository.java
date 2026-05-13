package com.asg.finance.repository;

import com.asg.finance.dto.JournalVoucherAssetCapitalizationResponseDto;
import com.asg.finance.dto.JournalVoucherAssetDetailDto;
import java.util.List;

public interface JournalVoucherProcRepository {

    List<JournalVoucherAssetDetailDto> fetchAssetDepreciationDetails(Long faPoid);
    
    List<JournalVoucherAssetCapitalizationResponseDto> fetchFixedAssetDetails(Long faPoid);
    
    void updateAssetDetail(Long transactionPoid);
}
