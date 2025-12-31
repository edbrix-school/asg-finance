package com.asg.finance.repository;

import com.asg.finance.entity.PettyCashUserroleMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PettyCashUserroleMasterRepository extends JpaRepository<PettyCashUserroleMaster, Long> {
}
