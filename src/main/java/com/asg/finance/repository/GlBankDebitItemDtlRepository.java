package com.asg.finance.repository;

import com.asg.finance.entity.GlBankDebitItemDtl;
import com.asg.finance.entity.key.GlBankDebitItemDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface GlBankDebitItemDtlRepository extends JpaRepository<GlBankDebitItemDtl, GlBankDebitItemDtlId> {
    
    List<GlBankDebitItemDtl> findByIdTransactionPoid(Long transactionPoid);
    
    @Query("SELECT MAX(g.id.detRowId) FROM GlBankDebitItemDtl g WHERE g.id.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
    
    void deleteByIdTransactionPoidAndIdDetRowIdNotIn(Long transactionPoid, Set<Long> detRowIds);
    
    default void deleteByTransactionPoidAndDetRowIdNotIn(Long transactionPoid, Set<Long> detRowIds) {
        deleteByIdTransactionPoidAndIdDetRowIdNotIn(transactionPoid, detRowIds);
    }
}