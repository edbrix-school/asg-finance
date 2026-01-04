package com.asg.finance.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.PurchaseOrderRequest;
import com.asg.finance.dto.PurchaseOrderResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface PurchaseOrderService {

    PurchaseOrderResponse createGeneralPurchaseOrder(String documentId, PurchaseOrderRequest request);

    PurchaseOrderResponse updatePurchaseOrder(String documentId, Long transactionPoid,
                                              PurchaseOrderRequest request);

    PurchaseOrderResponse findById(Long transactionPoid);

    void deletePurchaseOrder(Long transactionPoid);

    Map<String, Object> listPurchaseOrder(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);

    String createPOFromRFQ(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            Long poPoid,
            String supplierPoid,
            String rfqPoid
    );

    byte[] print(Long transactionPoid) throws Exception;

}
