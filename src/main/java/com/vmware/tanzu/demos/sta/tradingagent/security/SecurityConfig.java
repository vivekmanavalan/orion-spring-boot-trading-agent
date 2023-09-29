package com.vmware.tanzu.demos.sta.tradingagent.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.web.client.RestTemplateRequestCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
class SecurityConfig {
    private final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    @Profile("!test")
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        final var publicUrls = new String[]{
                // Page views.
                "/**",
                // Default page for errors.
                "/error",
                // Probes used by Knative.
                "/readyz", "/livez",
        };
        http.authorizeHttpRequests(
                        (auth) -> auth.requestMatchers(publicUrls).permitAll()
                                .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
                                .anyRequest().authenticated())
                .oauth2Client(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Profile("!test")
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {
        final var authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        final var authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientRepository);
        authorizedClientManager
                .setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    @Bean
    @Profile("!test")
    RestTemplateRequestCustomizer<ClientHttpRequest> oauth2Customizer(
            @Value("${spring.application.name}") String appName,
            OAuth2AuthorizedClientManager oauth2ClientManager) {
        return request -> {
            final var authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("sso")
                    .principal(appName)
                    .build();
            final var authorizedClient = oauth2ClientManager
                    .authorize(authorizeRequest);
            if (authorizedClient != null) {
                final var accessToken = authorizedClient.getAccessToken();
                logger.debug("Got OAuth2 token: {}", accessToken.getTokenValue());
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getTokenValue());
            }
        };
    }

    @Bean
    @Profile("test")
    SecurityFilterChain securityFilterChainTest(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(
                        (auth) -> auth.anyRequest().permitAll())
                .build();
    }
}
