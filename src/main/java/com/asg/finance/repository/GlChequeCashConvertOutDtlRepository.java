package com.asg.finance.repository;

import com.asg.finance.entity.GlChequeCashConvertOutDtlEntity;
import com.asg.finance.entity.key.GlChequeCashConvertOutDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface GlChequeCashConvertOutDtlRepository extends JpaRepository<GlChequeCashConvertOutDtlEntity, GlChequeCashConvertOutDtlKey> {

    List<GlChequeCashConvertOutDtlEntity> findByIdTransactionPoid(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(u.id.detRowId), 0) + 1 FROM GlChequeCashConvertOutDtlEntity u WHERE u.id.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(Long transactionPoid);

    void deleteByIdTransactionPoid(Long transactionPoid);

}
