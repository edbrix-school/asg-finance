package com.asg.finance.repository;

import com.asg.finance.entity.ArDebitNoteDtl;
import com.asg.finance.entity.ArDebitNoteDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArDebitNoteDtlRepository extends JpaRepository<ArDebitNoteDtl, ArDebitNoteDtlId> {

    List<ArDebitNoteDtl> findByTransactionPoid(Long transactionPoid);

    void deleteByTransactionPoid(Long transactionPoid);

    @Modifying
    @Query("DELETE FROM ArDebitNoteDtl d WHERE d.transactionPoid = :transactionPoid AND d.detRowId IN :detRowIds")
    void deleteByTransactionPoidAndDetRowIdIn(@Param("transactionPoid") Long transactionPoid, @Param("detRowIds") List<Long> detRowIds);

    @Query("SELECT MAX(d.detRowId) FROM ArDebitNoteDtl d WHERE d.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(Long transactionPoid);
}