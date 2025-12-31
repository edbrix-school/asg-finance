package com.asg.finance.repository;

import com.asg.finance.entity.GlImcoChequeRefundDtl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GlImcoChequeRefundDtlRepository extends JpaRepository<GlImcoChequeRefundDtl, Long> {
    List<GlImcoChequeRefundDtl> findByTransactionPoid(Long transactionPoid);
    Optional<GlImcoChequeRefundDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);
}