package com.asg.finance.repository;

import com.asg.finance.entity.GlPettyCashPaymentGrnDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlPettyCashPaymentGrnDtlRepository extends JpaRepository<GlPettyCashPaymentGrnDtl, GlPettyCashPaymentGrnDtl.CompositeKey> {
    List<GlPettyCashPaymentGrnDtl> findByTransactionPoid(Long transactionPoid);
}
