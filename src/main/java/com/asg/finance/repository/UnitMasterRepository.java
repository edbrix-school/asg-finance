package com.asg.finance.repository;

import com.asg.finance.entity.master.UnitMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnitMasterRepository extends JpaRepository<UnitMaster, Long> {
    Optional<UnitMaster> findByUnitPoid(Long unitPoid);
}
