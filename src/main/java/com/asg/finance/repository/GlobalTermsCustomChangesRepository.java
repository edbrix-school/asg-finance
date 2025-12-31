package com.asg.finance.repository;

import com.asg.finance.entity.GlobalTermsCustomChanges;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlobalTermsCustomChangesRepository
        extends JpaRepository<GlobalTermsCustomChanges, GlobalTermsCustomChanges.GlobalTermsCustomChangesId> {
    List<GlobalTermsCustomChanges> findByIdDocKeyPoid(Long docKeyPoid);
    List<GlobalTermsCustomChanges> findByIdDocIdAndIdDocKeyPoid(String docId, Long docKeyPoid);
}
