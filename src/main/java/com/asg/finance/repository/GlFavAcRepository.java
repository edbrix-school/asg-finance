package com.asg.finance.repository;

import com.asg.finance.entity.GlFavAcEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlFavAcRepository extends JpaRepository<GlFavAcEntity, Long> {

    GlFavAcEntity findByFavAcPoid(Long favAcPoid);




}
