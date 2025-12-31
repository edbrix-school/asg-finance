package com.asg.finance.repository;

import com.asg.finance.entity.GlobalLogSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalLogSummaryRepository extends JpaRepository<GlobalLogSummary, Long> {
}