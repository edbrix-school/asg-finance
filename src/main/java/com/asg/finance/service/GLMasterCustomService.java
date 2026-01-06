package com.asg.finance.service;

import com.asg.common.lib.dto.request.DocReleaseLockRequestDto;
import com.asg.finance.dto.GlLedgerDTO;

public interface GLMasterCustomService {

    String acquireLock(DocReleaseLockRequestDto request);

    String releaseLock(DocReleaseLockRequestDto request);
}
