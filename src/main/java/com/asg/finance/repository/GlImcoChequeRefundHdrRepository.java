package com.asg.finance.repository;

import com.asg.finance.entity.GlImcoChequeRefundHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GlImcoChequeRefundHdrRepository extends JpaRepository<GlImcoChequeRefundHdr, Long> {
    Optional<GlImcoChequeRefundHdr> findByTransactionPoid(Long transactionPoid);

    @Query(value = "SELECT COUNT(1) FROM GL_IMCO_CHEQUE_REFUND_HDR " +
            "WHERE RECEIPT_NUM = :receiptNum AND DELETED = 'N'", nativeQuery = true)
    long countByReceiptNum(@Param("receiptNum") String receiptNum);
}