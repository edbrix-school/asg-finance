package com.asg.finance.client;

import com.asg.common.lib.client.GenericRestClient;
import com.asg.common.lib.dto.GlPostingRequestDto;
import com.asg.common.lib.dto.response.ApiResponseWrapper;
import com.asg.common.lib.utility.RestClientUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GlPostingServiceClient {
    
    private final GenericRestClient restClient;
    
    @Value("${common-services.url:http://localhost:8083/common-services/api}")
    private String commonServicesUrl;
    
    public String glreposting(GlPostingRequestDto request) {
        String url = commonServicesUrl + "/v1/gl-postings";
        ApiResponseWrapper<String> response = restClient.post(url, request, new ParameterizedTypeReference<>() {});
        return RestClientUtil.extractData(response);
    }
}
