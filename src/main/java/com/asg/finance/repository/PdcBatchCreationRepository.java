package com.asg.finance.repository;

import com.asg.finance.dto.*;

public interface PdcBatchCreationRepository {

    PayGlBreakupCheckResponseDto checkPayGlBreakup(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            Long payGlPoid
    );

    PdcBatchCreationProcResponse runBatchCreation(PdcBatchCreationProcRequest request);

    PdcBatchCreationProcResponse runBankPosting(PdcBankPostingProcRequest request);

    PdcBatchCreationProcResponse runBatchCreationXL(PdcBatchCreationExcelProcRequest request);
}
