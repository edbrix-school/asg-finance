package com.asg.finance.repository;

import com.asg.finance.entity.GlJournalFaCapitalization;
import com.asg.finance.entity.key.TransactionDetailKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GlJournalFaCapitalizationRepository extends JpaRepository<GlJournalFaCapitalization, TransactionDetailKey>, JpaSpecificationExecutor<GlJournalFaCapitalization> {
}
