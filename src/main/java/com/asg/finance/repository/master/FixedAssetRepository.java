package com.asg.finance.repository.master;

import com.asg.finance.entity.master.FixedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Map;
import java.util.Optional;

public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long> {

    Optional<FixedAsset> findByFaPoid(Long faPoid);
    boolean existsByFaCode(String faCode);
    boolean existsByFaDescription(String faDescription);
    boolean existsByFaCodeAndFaPoidNot(String faCode, Long faPoid);
    boolean existsByFaDescriptionAndFaPoidNot(String faDescription, Long faPoid);
}
