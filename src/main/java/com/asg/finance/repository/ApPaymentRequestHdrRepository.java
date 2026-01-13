package com.asg.finance.repository;

import com.asg.finance.entity.ApPaymentRequestHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApPaymentRequestHdrRepository extends JpaRepository<ApPaymentRequestHdr, Long> {

    Optional<ApPaymentRequestHdr> findByTransactionPoid(Long transactionPoid);
}
