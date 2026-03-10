package com.asg.finance.repository;

import com.asg.finance.entity.GlJournalVoucherAssetDtl;
import com.asg.finance.entity.key.TransactionDetailKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface GlJournalVoucherAssetDtlRepository extends JpaRepository<GlJournalVoucherAssetDtl, TransactionDetailKey>, JpaSpecificationExecutor<GlJournalVoucherAssetDtl> {
    List<GlJournalVoucherAssetDtl> findByTransactionPoid(Long transactionPoid);
    void deleteByTransactionPoid(Long transactionPoid);
}
