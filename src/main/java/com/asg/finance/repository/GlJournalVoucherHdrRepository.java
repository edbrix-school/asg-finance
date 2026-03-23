package com.asg.finance.repository;

import com.asg.finance.entity.GlJournalVoucherHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


import java.util.Optional;

public interface GlJournalVoucherHdrRepository extends JpaRepository<GlJournalVoucherHdr, Long>, JpaSpecificationExecutor<GlJournalVoucherHdr>, JournalVoucherProcRepository {
    Optional<GlJournalVoucherHdr> findByTransactionPoid(Long transactionPoid);

}
