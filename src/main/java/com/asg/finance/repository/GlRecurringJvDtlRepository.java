package com.asg.finance.repository;

import com.asg.finance.entity.GlRecurringJvDtl;
import com.asg.finance.entity.key.TransactionDetailKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface GlRecurringJvDtlRepository extends JpaRepository<GlRecurringJvDtl, TransactionDetailKey>,
        JpaSpecificationExecutor<GlRecurringJvDtl> {

    List<GlRecurringJvDtl> findByTransactionPoid(Long transactionPoid);


    @Query("SELECT COALESCE(SUM(d.drAmt), 0) FROM GlRecurringJvDtl d WHERE d.transactionPoid = :transactionPoid")
    BigDecimal getDrTotalByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT COALESCE(SUM(d.crAmt), 0) FROM GlRecurringJvDtl d WHERE d.transactionPoid = :transactionPoid")
    BigDecimal getCrTotalByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM GlRecurringJvDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowIdByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    void deleteByTransactionPoid(Long transactionPoid);
}
