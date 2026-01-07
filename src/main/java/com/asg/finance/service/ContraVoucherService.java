package com.asg.finance.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.ContraVoucherRequest;
import com.asg.finance.dto.ContraVoucherFullResponse;
import com.asg.finance.dto.ContraVoucherResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface ContraVoucherService {

    Map<String, Object> listContraVouchers(String docId, FilterRequestDto request, Pageable pageable, LocalDate periodFrom, LocalDate periodTo);

    ContraVoucherFullResponse createContraVoucher(ContraVoucherRequest request);

    ContraVoucherFullResponse updateContraVoucher(ContraVoucherRequest request);

    ContraVoucherFullResponse getContraVoucherById(Long transactionPoid);

    void deleteContraVoucher(Long transactionPoid);

    String checkGlNature(Long creditGlId);

    byte[] print(Long transactionPoid) throws Exception;
}
