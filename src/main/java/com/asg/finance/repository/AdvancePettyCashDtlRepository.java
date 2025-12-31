package com.asg.finance.repository;

import com.asg.finance.entity.AdvancePettyCashDtl;
import com.asg.finance.entity.AdvancePettyCashDtlId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvancePettyCashDtlRepository extends JpaRepository<AdvancePettyCashDtl, AdvancePettyCashDtlId> {
    List<AdvancePettyCashDtl> findByTransactionPoid(Long transactionPoid);
}