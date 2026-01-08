package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.finance.dto.BankPayeeRequest;
import com.asg.finance.dto.BankPayeeResponse;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface IBankPayeeService {
    BankPayeeResponse createPayee(BankPayeeRequest request);
    BankPayeeResponse getPayeeById(Long poid);
    void softDeleteBypPayingPoid(Long id, DeleteReasonDto deleteReasonDto);
    BankPayeeResponse updatePayee(Long payingPoid, BankPayeeRequest request);
    Map<String, Object> listPayees(String documentId, FilterRequestDto filters, Pageable pageable);
}
