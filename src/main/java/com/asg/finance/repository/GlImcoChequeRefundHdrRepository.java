package com.asg.finance.repository;

import com.asg.finance.entity.GlImcoChequeRefundHdr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GlImcoChequeRefundHdrRepository extends JpaRepository<GlImcoChequeRefundHdr, Long> {
    Optional<GlImcoChequeRefundHdr> findByTransactionPoid(Long transactionPoid);
    boolean existsByReceiptNum(String receiptNumber);
}