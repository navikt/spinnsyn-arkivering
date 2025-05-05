package no.nav.helse.flex.config

import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.util.function.Supplier

@EnableJwtTokenValidation
@EnableOAuth2Client(cacheEnabled = true)
@Configuration
class AadRestTemplateConfiguration {
    companion object {
        fun lagRequestFactoryUtenProtocolUpgrade(): HttpComponentsClientHttpRequestFactory {
            val requestConfig =
                RequestConfig
                    .custom()
                    .setProtocolUpgradeEnabled(false)
                    .build()

            val httpClient =
                HttpClientBuilder
                    .create()
                    .setDefaultRequestConfig(requestConfig)
                    .build()

            return HttpComponentsClientHttpRequestFactory(httpClient)
        }
    }

    @Bean
    fun spinnsynFrontendArkiveringRestTemplate(
        restTemplateBuilder: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService,
    ): RestTemplate {
        val registrationName = "spinnsyn-frontend-arkivering-credentials"
        val clientProperties =
            clientConfigurationProperties.registration[registrationName]
                ?: throw RuntimeException("Fant ikke config for $registrationName")

        return restTemplateBuilder
            .requestFactory(Supplier { lagRequestFactoryUtenProtocolUpgrade() })
            .additionalInterceptors(bearerTokenInterceptor(clientProperties, oAuth2AccessTokenService))
            .build()
    }

    @Bean
    fun dokarkivRestTemplate(
        restTemplateBuilder: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService,
    ): RestTemplate {
        val registrationName = "dokarkiv-client-credentials"
        val clientProperties =
            clientConfigurationProperties.registration[registrationName]
                ?: throw RuntimeException("Fant ikke config for $registrationName")
        return restTemplateBuilder
            .additionalInterceptors(bearerTokenInterceptor(clientProperties, oAuth2AccessTokenService))
            .build()
    }

    private fun bearerTokenInterceptor(
        clientProperties: ClientProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService,
    ): ClientHttpRequestInterceptor =
        ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
            response.access_token?.let { request.headers.setBearerAuth(it) }
            execution.execute(request, body)
        }
}
