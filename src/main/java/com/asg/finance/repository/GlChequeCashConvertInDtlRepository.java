package com.asg.finance.repository;

import com.asg.finance.entity.GlChequeCashConvertInDtlEntity;
import com.asg.finance.entity.key.GlChequeCashConvertInDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface GlChequeCashConvertInDtlRepository extends JpaRepository<GlChequeCashConvertInDtlEntity, GlChequeCashConvertInDtlKey> {

    List<GlChequeCashConvertInDtlEntity> findByIdTransactionPoid(Long transactionPoid);

    @Query("SELECT e FROM GlChequeCashConvertInDtlEntity e WHERE e.id.transactionPoid = :transactionPoid AND e.id.detRowId = :detRowId")
    Optional<GlChequeCashConvertInDtlEntity> findByIdTransactionPoidAndIdDetRowId(@Param("transactionPoid") Long transactionPoid, @Param("detRowId") Long detRowId);

    void deleteByIdTransactionPoid(Long transactionPoid);

    @Modifying
    @Query("DELETE FROM GlChequeCashConvertInDtlEntity e WHERE e.id.transactionPoid = :transactionPoid AND e.id.detRowId IN :detRowIds")
    void deleteByIdTransactionPoidAndIdDetRowIdIn(@Param("transactionPoid") Long transactionPoid, @Param("detRowIds") List<Long> detRowIds);

    @Query("SELECT COALESCE(MAX(u.id.detRowId), 0) + 1 FROM GlChequeCashConvertInDtlEntity u WHERE u.id.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(Long transactionPoid);


}
