package com.asg.finance.repository;

import com.asg.finance.entity.PdcChqBatchHdrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PdcChqBatchHdrRepository extends JpaRepository<PdcChqBatchHdrEntity, Long> {
    boolean existsByTransactionPoid(Long transactionPoid);
}
