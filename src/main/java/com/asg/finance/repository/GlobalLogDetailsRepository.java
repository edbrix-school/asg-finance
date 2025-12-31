package com.asg.finance.repository;

import com.asg.finance.entity.GlobalLogDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalLogDetailsRepository extends JpaRepository<GlobalLogDetails, Long> {
}