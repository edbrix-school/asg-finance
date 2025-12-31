package com.asg.finance.repository;

import com.asg.finance.entity.GlBankDebitChargeDtl;
import com.asg.finance.entity.key.GlBankDebitChargeDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface GlBankDebitChargeDtlRepository extends JpaRepository<GlBankDebitChargeDtl, GlBankDebitChargeDtlId> {
    
    List<GlBankDebitChargeDtl> findByIdTransactionPoid(Long transactionPoid);
    
    @Query("SELECT MAX(g.id.detRowId) FROM GlBankDebitChargeDtl g WHERE g.id.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
    
    void deleteByIdTransactionPoidAndIdDetRowIdNotIn(Long transactionPoid, Set<Long> detRowIds);
    
    default void deleteByTransactionPoidAndDetRowIdNotIn(Long transactionPoid, Set<Long> detRowIds) {
        deleteByIdTransactionPoidAndIdDetRowIdNotIn(transactionPoid, detRowIds);
    }
}