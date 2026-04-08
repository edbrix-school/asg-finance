package com.asg.finance.repository;


import com.asg.finance.entity.PdcChqBatchDtlEntity;
import com.asg.finance.entity.PdcChqBatchDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface PdcChqBatchDtlRepository extends JpaRepository<PdcChqBatchDtlEntity, PdcChqBatchDtlId> {
    PdcChqBatchDtlEntity findTopByTransactionPoidOrderByDetRowIdDesc(Long transactionPoid);
    List<PdcChqBatchDtlEntity> findByTransactionPoidOrderByDetRowIdAsc(Long transactionPoid);
    @Modifying
    @Query("DELETE FROM PdcChqBatchDtlEntity d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
    @Modifying
    @Query("DELETE FROM PdcChqBatchDtlEntity d WHERE d.transactionPoid = :transactionPoid AND d.detRowId = :detRowId")
    void deleteByTransactionPoidAndDetRowId(@Param("transactionPoid") Long transactionPoid, @Param("detRowId") Long detRowId);
    @Modifying
    @Query("DELETE FROM PdcChqBatchDtlEntity d WHERE d.transactionPoid = :transactionPoid AND d.detRowId NOT IN :detRowIds")
    void deleteByTransactionPoidAndDetRowIdNotIn(@Param("transactionPoid") Long transactionPoid, @Param("detRowIds") Set<Long> detRowIds);
}
