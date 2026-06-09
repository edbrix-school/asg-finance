package com.asg.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Base interface for Petty Cash request DTOs.
 * Both create and update DTOs share the same fields structure.
 */
public interface PettyCashRequestBase {
    LocalDate getTransactionDate();
    String getCurrencyCode();
    BigDecimal getCurrencyRate();
    Long getPettyCashGlPoid();
    BigDecimal getBalance();
    BigDecimal getAmount();
    String getPayingTo();
    String getNarration();
    String getAdvance();
    String getRefType();
    String getFdaRef();
    String getFfRef();
    default List<String> getFfRefs() { return null; }
    LocalDate getSettledDate();
    String getRemarks();
    BigDecimal getSettledTotal();
    String getStatus();
    BigDecimal getGrandTotal();
    String getMtaRef();
    String getMultiCompany();
    String getPoRef();
    String getSalesQtnRef();
    BigDecimal getCrTotal();
    BigDecimal getDrTotal();
    BigDecimal getRoundingAmount();
    Long getGrnSupplierPoid();
    Long getSupplierGlPoid();
    Long getCustomerGlPoid();
    Long getAdvancePettyCashPoid();
    String getAdvanceStatus();
    BigDecimal getAdvanceAmount();
    Long getCompanyDivPoid();
    List<GlPettyCashPaymentDtlRequestDto> getGlPettyCashPaymentDtlRequestDtos();
    List<GlPettyCashItemDtlRequestDto> getGlPettyCashItemDtlRequestDtos();
    List<GlPettyCashChargeDtlRequestDto> getGlPettyCashChargeDtlRequestDtos();
    List<GlPettyCashPaymentGrnDtlRequestDto> getGlPettyCashGrnDtlRequestDtos();
    String getDocId();
    Long getBookPoid();
}

