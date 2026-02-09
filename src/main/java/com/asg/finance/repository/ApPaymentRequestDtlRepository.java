package com.asg.finance.repository;

import com.asg.finance.entity.ApPaymentRequestDtl;
import com.asg.finance.entity.ApPaymentRequestDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApPaymentRequestDtlRepository extends JpaRepository<ApPaymentRequestDtl, ApPaymentRequestDtlId> {
    List<ApPaymentRequestDtl> findByIdTransactionPoid(Long transactionPoid);
    
    Optional<ApPaymentRequestDtl> findByIdTransactionPoidAndIdDetRowId(Long transactionPoid, Long detRowId);

    void deleteByIdTransactionPoid(Long transactionPoid);

}
