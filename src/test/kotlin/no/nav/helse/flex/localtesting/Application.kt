package no.nav.helse.flex.localtesting

import jakarta.servlet.http.HttpServletResponse
import no.nav.helse.flex.arkivering.PdfSkaperen
import no.nav.helse.flex.client.SpinnsynFrontendArkiveringClient
import no.nav.helse.flex.config.AadRestTemplateConfiguration
import no.nav.helse.flex.html.HtmlInliner
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.client.RestTemplate
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import java.util.function.Supplier

@SpringBootApplication(
    exclude = [DataSourceAutoConfiguration::class],
)
// Bruker annen profil for å skille Application fra Application i no.nav.helse.flex etter oppgradering til
// Spring Boot 3.2.0. Husk å angi at ActiveProfile er "localtesting" i Run Configuration.
@Profile("localtesting")
class Application

fun main(args: Array<String>) {
    VeraGreenfieldFoundryProvider.initialise()
    runApplication<Application>(*args)
}

@Controller
@Unprotected
class TestController {
    final val pdfSkaperen: PdfSkaperen

    init {
        val url = "http://localhost:8080"

        val htmlInliner = HtmlInliner(url)
        val spinnsynFrontendArkiveringClient =
            SpinnsynFrontendArkiveringClient(
                spinnsynFrontendArkiveringRestTemplate = lagRestTemplate(),
                url = url,
            )
        pdfSkaperen = PdfSkaperen(spinnsynFrontendArkiveringClient, htmlInliner)
    }

    @ResponseBody
    @GetMapping(value = ["/api/test", "/api/test/"], produces = [MediaType.TEXT_HTML_VALUE])
    fun hentHtml(response: HttpServletResponse): String {
        val hentSomHtmlOgInlineTing =
            pdfSkaperen.hentSomHtmlOgInlineTing(fnr = "12345554488", id = "utvikling-arkivering")
        return hentSomHtmlOgInlineTing.html
    }

    @ResponseBody
    @GetMapping(value = ["/api/test/pdf/", "/api/test/pdf"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun hentPdf(response: HttpServletResponse): ByteArray {
        val hentPdf = pdfSkaperen.hentPdf(fnr = "12345554488", id = "utvikling-arkivering")
        response.setHeader("x-nais-app-image", hentPdf.versjon)

        return hentPdf.pdf
    }

    // Lager RestTemplate som ikke gjør protocol upgrade: https://nav-it.slack.com/archives/C5KUST8N6/p1736954170766729
    fun lagRestTemplate(): RestTemplate =
        RestTemplateBuilder()
            .requestFactory(Supplier { AadRestTemplateConfiguration.lagRequestFactoryUtenProtocolUpgrade() })
            .build()
}
