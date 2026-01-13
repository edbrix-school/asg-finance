package com.asg.finance.repository;

import com.asg.finance.entity.ApPaymentRequestDtl;
import com.asg.finance.entity.ApPaymentRequestDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApPaymentRequestDtlRepository extends JpaRepository<ApPaymentRequestDtl, ApPaymentRequestDtlId> {
    List<ApPaymentRequestDtl> findByIdTransactionPoid(Long transactionPoid);

    void deleteByIdTransactionPoid(Long transactionPoid);

}
