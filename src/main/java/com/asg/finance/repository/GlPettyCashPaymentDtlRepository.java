package com.asg.finance.repository;


import com.asg.finance.entity.GlPettyCashPaymentDtl;
import jdk.jfr.Registered;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

@Registered
public interface GlPettyCashPaymentDtlRepository extends JpaRepository<GlPettyCashPaymentDtl, Long> {

    List<GlPettyCashPaymentDtl> findByTransactionPoid(Long transactionPoid);


    }


