package com.asg.finance.client;

import com.asg.common.lib.client.GenericRestClient;
import com.asg.common.lib.dto.AddressMasterUpsertDto;
import com.asg.common.lib.dto.response.AddressMasterResponse;
import com.asg.common.lib.dto.response.ApiResponseWrapper;
import com.asg.common.lib.utility.RestClientUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddressMasterServiceClient {
    
    private final GenericRestClient restClient;
    
    @Value("${settings.service.url:http://localhost:8084/settings/api/}")
    private String settingsServiceUrl;
    
    public AddressMasterResponse findById(Long addressPoid) {
        String url = settingsServiceUrl + "/v1/address-master/simple/" + addressPoid;
        ApiResponseWrapper<AddressMasterResponse> response = restClient.get(url,
            new ParameterizedTypeReference<ApiResponseWrapper<AddressMasterResponse>>() {});
        return RestClientUtil.extractData(response);
    }
    
    public AddressMasterResponse upsert(AddressMasterUpsertDto request) {
        String url = settingsServiceUrl + "/v1/address-master/upsert";
        ApiResponseWrapper<AddressMasterResponse> response = restClient.post(url, request,
            new ParameterizedTypeReference<ApiResponseWrapper<AddressMasterResponse>>() {});
        return RestClientUtil.extractData(response);
    }
}
