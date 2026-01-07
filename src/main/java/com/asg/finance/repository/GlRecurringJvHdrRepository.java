package com.asg.finance.repository;

import com.asg.finance.entity.GlRecurringJvHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlRecurringJvHdrRepository extends JpaRepository<GlRecurringJvHdr, Long>,
        JpaSpecificationExecutor<GlRecurringJvHdr> {

    Optional<GlRecurringJvHdr> findByTransactionPoid(Long transactionPoid);

}

