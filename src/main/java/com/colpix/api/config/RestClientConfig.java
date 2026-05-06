package com.colpix.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
public class RestClientConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Backend RestClient pre-configured with the base URL and timeouts. All
     * outbound calls to the backend products service go through this bean.
     */
    @Bean
    public RestClient backendRestClient(AppProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.backend().connectTimeout().toMillis());
        factory.setReadTimeout((int) properties.backend().readTimeout().toMillis());

        return RestClient.builder()
                .baseUrl(properties.backend().baseUrl())
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
