package com.asg.finance.repository;

import com.asg.finance.entity.GLPettyCashItemDtl;
import jdk.jfr.Registered;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@Registered
public interface GLPettyCashItemDtlRepository extends JpaRepository<GLPettyCashItemDtl, Long> {

    List<GLPettyCashItemDtl> findByTransactionPoid(Long transactionPoid);
}
