package com.asg.finance.repository.master;

import com.asg.finance.entity.master.HrEmployeeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HrEmployeeMasterRepository extends JpaRepository<HrEmployeeMaster, Long> {
    Optional<HrEmployeeMaster> findByEmployeePoid(Long employeePoid);

    boolean existsByEmployeePoid(Long employeePoid);
}
