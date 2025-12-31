package com.asg.finance.repository;

import com.asg.finance.entity.GlobalTaxPeriodChargeDtlEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlobalTaxPeriodChargeDtlRepository extends JpaRepository<GlobalTaxPeriodChargeDtlEntity, Long> {
    Page<GlobalTaxPeriodChargeDtlEntity> findByTransactionPoid(Long transactionPoid, Pageable pageable);
    Optional<GlobalTaxPeriodChargeDtlEntity> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);
    void deleteByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);
    void deleteByTransactionPoidAndDetRowIdIn(Long transactionPoid, List<Long> detRowIds);
}
