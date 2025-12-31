package com.asg.finance.repository;

import com.asg.finance.entity.GlImcoChequeBillDtl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GlImcoChequeBillDtlRepository extends JpaRepository<GlImcoChequeBillDtl, Long> {
    List<GlImcoChequeBillDtl> findByTransactionPoid(Long transactionPoid);
}