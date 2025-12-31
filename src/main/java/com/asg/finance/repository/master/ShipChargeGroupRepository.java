package com.asg.finance.repository.master;

import com.asg.finance.entity.master.ShipChargeGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ShipChargeGroupRepository extends JpaRepository<ShipChargeGroupEntity, Long> {
    Optional<ShipChargeGroupEntity> findByChargeGroupPoid(Long chargeGroupPoid);
    List<ShipChargeGroupEntity> findByChargeGroupPoidIn(Set<Long> chargeGroupPoids);
}
