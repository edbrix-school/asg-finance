package com.asg.finance.repository;

import com.asg.finance.entity.GlBankEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlBankRepository extends JpaRepository<GlBankEntity, Long> {
    GlBankEntity findByBankPoid(Long bankPoid);

    boolean existsByBankPoid(Long bankPoid);

    boolean existsByBankCodeIgnoreCase(String bankCode);

    boolean existsByBankCodeIgnoreCaseAndBankPoidNot(String bankCode, Long bankPoid);

    boolean existsByBankDescriptionIgnoreCase(@NotBlank(message = "bankName is required") @Size(max = 100, message = "bankDescription must be at most 100 characters") String bankDescription);

    boolean existsByBankDescriptionIgnoreCaseAndBankPoidNot(@NotBlank(message = "bankName is required") @Size(max = 100, message = "bankDescription must be at most 100 characters") String bankDescription, Long bankPoid);

    boolean existsByBankAccountNoIgnoreCase(@NotBlank(message = "bankAccountNo is required") @Size(max = 100, message = "bankAccountNo must be at most 100 characters") String bankAccountNo);

    boolean existsByBankAccountNoIgnoreCaseAndBankPoidNot(@NotBlank(message = "bankAccountNo is required") @Size(max = 100, message = "bankAccountNo must be at most 100 characters") String bankAccountNo, Long bankPoid);
}

