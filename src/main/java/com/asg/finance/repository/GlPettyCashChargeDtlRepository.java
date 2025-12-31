package com.asg.finance.repository;

import com.asg.finance.entity.GlPettyCashChargeDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlPettyCashChargeDtlRepository extends JpaRepository<GlPettyCashChargeDtl, Long> {

    List<GlPettyCashChargeDtl> findByTransactionPoid(Long transactionPoid);
}
