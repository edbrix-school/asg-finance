package com.asg.finance.repository;


import com.asg.finance.entity.GlRecurringJvMonthDtl;
import com.asg.finance.entity.key.TransactionDetailKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface GlRecurringJvMonthDtlRepository extends JpaRepository<GlRecurringJvMonthDtl, TransactionDetailKey>,
        JpaSpecificationExecutor<GlRecurringJvMonthDtl> {

    List<GlRecurringJvMonthDtl> findByTransactionPoid(Long transactionPoid);


    List<GlRecurringJvMonthDtl> findByTransactionPoidAndStatus(Long transactionPoid, String status);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM GlRecurringJvMonthDtl m WHERE m.transactionPoid = :transactionPoid")
    BigDecimal getTotalAmountByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT COUNT(m) FROM GlRecurringJvMonthDtl m WHERE m.transactionPoid = :transactionPoid AND m.status = 'CREATED'")
    Long countCreatedSchedulesByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    void deleteByTransactionPoid(Long transactionPoid);
}

