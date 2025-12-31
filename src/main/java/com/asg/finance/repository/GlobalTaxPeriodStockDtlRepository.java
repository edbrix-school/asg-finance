package com.asg.finance.repository;


import com.asg.finance.entity.GlobalTaxPeriodStockDtlEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GlobalTaxPeriodStockDtlRepository extends JpaRepository<GlobalTaxPeriodStockDtlEntity, Long> {
    Page<GlobalTaxPeriodStockDtlEntity> findByTransactionPoid(Long transactionPoid, Pageable pageable);
    Optional<GlobalTaxPeriodStockDtlEntity> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);
    void deleteByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);
    void deleteByTransactionPoidAndDetRowIdIn(Long transactionPoid, List<Long> detRowIds);
}
