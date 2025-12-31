package com.asg.finance.repository;

import com.asg.finance.entity.GlChequeCashConvertInDtlEntity;
import com.asg.finance.entity.key.GlChequeCashConvertInDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface GlChequeCashConvertInDtlRepository extends JpaRepository<GlChequeCashConvertInDtlEntity, GlChequeCashConvertInDtlKey> {

    List<GlChequeCashConvertInDtlEntity> findByIdTransactionPoid(Long transactionPoid);

    void deleteByIdTransactionPoid(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(u.id.detRowId), 0) + 1 FROM GlChequeCashConvertInDtlEntity u WHERE u.id.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(Long transactionPoid);


}
