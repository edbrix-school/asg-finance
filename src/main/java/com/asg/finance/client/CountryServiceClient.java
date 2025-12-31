package com.asg.finance.client;

import com.asg.common.lib.client.GenericRestClient;
import com.asg.common.lib.dto.CompanyDto;
import com.asg.common.lib.dto.CountryDto;
import com.asg.common.lib.dto.CountrySimpleDto;
import com.asg.common.lib.dto.response.AddressMasterResponse;
import com.asg.common.lib.dto.response.ApiResponseWrapper;
import com.asg.common.lib.utility.RestClientUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CountryServiceClient {
    
    private final GenericRestClient restClient;
    
    @Value("${settings.service.url:http://localhost:8084/setting/api}")
    private String settingsServiceUrl;
    
    public CountryDto findById(Long countryPoid) {
        String url = settingsServiceUrl + "/v1/country-master/" + countryPoid;
        ApiResponseWrapper<CountryDto> response = restClient.get(url,  new ParameterizedTypeReference<ApiResponseWrapper<CountryDto>>() {});
        return RestClientUtil.extractData(response);
    }
}
