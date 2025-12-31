package com.asg.finance.client;

import com.asg.common.lib.client.GenericRestClient;
import com.asg.common.lib.dto.request.GlobalTermsInsertRequestDto;
import com.asg.common.lib.dto.response.ApiResponseWrapper;
import com.asg.common.lib.dto.response.GlobalTermsResponseDto;
import com.asg.common.lib.utility.RestClientUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GlobalTermsServiceClient {
    
    private final GenericRestClient restClient;
    
    @Value("${common-services.url:http://localhost:8083/common-services/api}")
    private String commonServicesUrl;
    
    public void insertGlobalTerms(List<GlobalTermsInsertRequestDto> requestList) {
        String url = commonServicesUrl + "/v1/common/term-condition/insert";
        restClient.post(url, requestList, Void.class);
    }

    public GlobalTermsResponseDto loadGlobalTermsList(Long groupPoid, Long companyPoid, String docId, Long docKeyPoid, Long termsPoid) {
        String url = commonServicesUrl + "/v1/common/term-condition/load" +
                "?documentId=" + docId + "&docKeyPoid=" + docKeyPoid +
                (termsPoid != null ? "&termsPoid=" + termsPoid : "");
        ApiResponseWrapper<GlobalTermsResponseDto> response = restClient.get(url, new ParameterizedTypeReference<>() {});
        return RestClientUtil.extractData(response);
    }

    public String createPoFromRfq(Long loginGroupPoid, Long loginUserPoid, Long loginCompanyPoid, Long poPoid, String supplierPoid, String rfqPoid) {
        String url = commonServicesUrl + "/v1/common/po/create-from-rfq" +
                "?poPoid=" + poPoid + "&supplierPoid=" + supplierPoid + "&rfqPoid=" + rfqPoid;
        ApiResponseWrapper<String> response = restClient.post(url, null, new ParameterizedTypeReference<>() {});
        return RestClientUtil.extractData(response);
    }
}
