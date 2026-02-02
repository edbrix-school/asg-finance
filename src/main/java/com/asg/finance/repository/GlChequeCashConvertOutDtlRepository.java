package com.asg.finance.repository;

import com.asg.finance.entity.GlChequeCashConvertOutDtlEntity;
import com.asg.finance.entity.key.GlChequeCashConvertOutDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface GlChequeCashConvertOutDtlRepository extends JpaRepository<GlChequeCashConvertOutDtlEntity, GlChequeCashConvertOutDtlKey> {

    List<GlChequeCashConvertOutDtlEntity> findByIdTransactionPoid(Long transactionPoid);

    @Query("SELECT e FROM GlChequeCashConvertOutDtlEntity e WHERE e.id.transactionPoid = :transactionPoid AND e.id.detRowId = :detRowId")
    Optional<GlChequeCashConvertOutDtlEntity> findByIdTransactionPoidAndIdDetRowId(@Param("transactionPoid") Long transactionPoid, @Param("detRowId") Long detRowId);

    @Modifying
    @Query("DELETE FROM GlChequeCashConvertOutDtlEntity e WHERE e.id.transactionPoid = :transactionPoid AND e.id.detRowId IN :detRowIds")
    void deleteByIdTransactionPoidAndIdDetRowIdIn(@Param("transactionPoid") Long transactionPoid, @Param("detRowIds") List<Long> detRowIds);

    @Query("SELECT COALESCE(MAX(u.id.detRowId), 0) + 1 FROM GlChequeCashConvertOutDtlEntity u WHERE u.id.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(Long transactionPoid);

    void deleteByIdTransactionPoid(Long transactionPoid);

}
