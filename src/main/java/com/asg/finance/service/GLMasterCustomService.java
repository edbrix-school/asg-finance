package com.asg.finance.service;

import com.asg.common.lib.dto.request.DocReleaseLockRequestDto;
import com.asg.finance.dto.GlLedgerDTO;

public interface GLMasterCustomService {
    GlLedgerDTO callProcGlMasterCreate(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            String loginUser,
            String code,
            String description,
            String glType
    );

    String acquireLock(DocReleaseLockRequestDto request);

    String releaseLock(DocReleaseLockRequestDto request);
}
