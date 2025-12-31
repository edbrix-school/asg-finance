package com.asg.finance.repository;

import com.asg.finance.entity.GlobalTaxSubmissionDtl;
import com.asg.finance.entity.GlobalTaxSubmissionDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlobalTaxSubmissionDtlRepository extends JpaRepository<GlobalTaxSubmissionDtl, GlobalTaxSubmissionDtlId>,
        JpaSpecificationExecutor<GlobalTaxSubmissionDtl> {

    List<GlobalTaxSubmissionDtl> findByTransactionPoid(Long transactionPoid);

    void deleteByTransactionPoid(Long transactionPoid);
}


