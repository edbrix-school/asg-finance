package com.asg.finance.service;

import com.asg.common.lib.dto.GlPostingRequestDto;
import com.asg.finance.client.GlPostingServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerPostingServiceImpl implements LedgerPostingService {

    private final GlPostingServiceClient glPostingServiceClient;

    @Override
    public void postToLedger(Long transactionPoid, String docId) {
        int groupPoid = 1;
        int companyPoid = 1;
        int userPoid = 0;
        int docRef = 0;

        try {
            log.info("Posting GL Ledger for TRANSACTION_POID={}, DOC_ID={}", transactionPoid, docId);
            
            GlPostingRequestDto request = GlPostingRequestDto.builder()
                    .loginGroupPoid(groupPoid)
                    .loginCompanyPoid(companyPoid)
                    .loginUserPoid(userPoid)
                    .docId(docId)
                    .transactionPoid(transactionPoid.intValue())
                    .docRef(docRef)
                    .build();
            
            String result = glPostingServiceClient.glreposting(request);

            if (result == null || !result.toUpperCase().contains("SUCCESS")) {
                throw new RuntimeException("GL Posting failed: " + result);
            }

            log.info("GL posting completed successfully: {}", result);
        } catch (Exception e) {
            log.error("GL posting error for TRANSACTION_POID {}: {}", transactionPoid, e.getMessage());
            throw new RuntimeException("Error posting to GL ledger: " + e.getMessage(), e);
        }
    }
}
