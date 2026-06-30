package com.asg.finance.repository;

import com.asg.finance.entity.GlImcoChequeRefundHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GlImcoChequeRefundHdrRepository extends JpaRepository<GlImcoChequeRefundHdr, Long> {
    Optional<GlImcoChequeRefundHdr> findByTransactionPoid(Long transactionPoid);

    @Query(value = "SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END FROM GL_IMCO_CHEQUE_REFUND_HDR " +
            "WHERE RECEIPT_NUM = :receiptNum AND DELETED = 'N'", nativeQuery = true)
    boolean existsByReceiptNum(@Param("receiptNum") String receiptNum);
}