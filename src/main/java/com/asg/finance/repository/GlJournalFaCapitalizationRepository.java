package com.asg.finance.repository;

import com.asg.finance.entity.GlJournalFaCapitalization;
import com.asg.finance.entity.key.TransactionDetailKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GlJournalFaCapitalizationRepository extends JpaRepository<GlJournalFaCapitalization, TransactionDetailKey>, JpaSpecificationExecutor<GlJournalFaCapitalization> {
    List<GlJournalFaCapitalization> findByTransactionPoid(Long transactionPoid);
    void deleteByTransactionPoid(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM GlJournalFaCapitalization d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowIdByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
}
