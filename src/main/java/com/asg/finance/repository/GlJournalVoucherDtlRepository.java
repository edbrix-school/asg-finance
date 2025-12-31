package com.asg.finance.repository;

import com.asg.finance.entity.GlJournalVoucherDtl;
import com.asg.finance.entity.key.TransactionDetailKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface GlJournalVoucherDtlRepository extends JpaRepository<GlJournalVoucherDtl, TransactionDetailKey>, JpaSpecificationExecutor<GlJournalVoucherDtl> {
    
    @Query("SELECT COALESCE(SUM(d.drAmt), 0) FROM GlJournalVoucherDtl d WHERE d.transactionPoid = :transactionPoid")
    BigDecimal sumDrAmtByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
    
    @Query("SELECT COALESCE(SUM(d.crAmt), 0) FROM GlJournalVoucherDtl d WHERE d.transactionPoid = :transactionPoid")
    BigDecimal sumCrAmtByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
    
    List<GlJournalVoucherDtl> findByTransactionPoid(Long transactionPoid);
}
