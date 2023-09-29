package com.vmware.tanzu.demos.sta.tradingagent.web;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateRequestCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
class RestTemplateConfig {
    private final Logger logger = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    RestTemplateBuilder restTemplateBuilder(@Value("${spring.application.name}") String appName,
                                            @Value("${app.marketplace.url}") String marketplaceUrl,
                                            @Autowired(required = false) RestTemplateRequestCustomizer<ClientHttpRequest> oauth2Customizer) {
        final var okhttpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        final var reqFactory = new OkHttp3ClientHttpRequestFactory(okhttpClient);

        logger.info("Using marketplace URL: {}", marketplaceUrl);
        var builder = new RestTemplateBuilder()
                .rootUri(marketplaceUrl)
                .requestFactory(() -> reqFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, appName);
        if (oauth2Customizer != null) {
            builder = builder.additionalRequestCustomizers(oauth2Customizer);
        }
        return builder;
    }
}
