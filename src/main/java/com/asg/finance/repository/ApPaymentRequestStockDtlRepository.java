package com.asg.finance.repository;

import com.asg.finance.entity.ApPaymentRequestDtlId;
import com.asg.finance.entity.ApPaymentRequestStockDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApPaymentRequestStockDtlRepository extends JpaRepository<ApPaymentRequestStockDtl, ApPaymentRequestDtlId> {
    List<ApPaymentRequestStockDtl> findByIdTransactionPoid(Long transactionPoid);
    Optional<ApPaymentRequestStockDtl> findByIdTransactionPoidAndIdDetRowId(Long transactionPoid, Long detRowId);
    void deleteByIdTransactionPoid(Long transactionPoid);
}
