package com.asg.finance.client;

import com.asg.common.lib.client.GenericRestClient;
import com.asg.common.lib.dto.CurrencySimpleDto;
import com.asg.common.lib.dto.response.ApiResponseWrapper;
import com.asg.common.lib.utility.RestClientUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrencyServiceClient {
    
    private final GenericRestClient restClient;
    
    @Value("${settings.service.url:http://localhost:8082/setting/api}")
    private String settingsServiceUrl;
    
    public CurrencySimpleDto findByCurrencyCode(String currencyCode) {
        String url = settingsServiceUrl + "/v1/currencies/simple/" + currencyCode;
        ApiResponseWrapper<CurrencySimpleDto> response = restClient.get(url, new ParameterizedTypeReference<>() {});
        return RestClientUtil.extractData(response);
    }

}
