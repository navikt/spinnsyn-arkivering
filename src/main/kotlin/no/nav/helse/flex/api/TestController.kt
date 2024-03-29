package no.nav.helse.flex.api

import jakarta.servlet.http.HttpServletResponse
import no.nav.helse.flex.arkivering.PdfSkaperen
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import java.util.UUID

@Controller
@Unprotected
@Profile("testcontroller")
class TestController(
    val arkivaren: PdfSkaperen,
    val environmentToggles: EnvironmentToggles,
) {
    fun dobbeltsjekkProd() {
        if (environmentToggles.isProduction()) {
            throw RuntimeException("Disablet i produksjon")
        }
    }

    @ResponseBody
    @GetMapping(value = ["/api/test/html/{fnr}/{utbetalingId}"], produces = [MediaType.TEXT_HTML_VALUE])
    fun hentHtml(
        @PathVariable fnr: String,
        @PathVariable utbetalingId: UUID,
        response: HttpServletResponse,
    ): String {
        dobbeltsjekkProd()
        val hentSomHtmlOgInlineTing = arkivaren.hentSomHtmlOgInlineTing(fnr = fnr, id = utbetalingId.toString())
        response.setHeader("x-nais-app-image", hentSomHtmlOgInlineTing.versjon)
        return hentSomHtmlOgInlineTing.html
    }

    @ResponseBody
    @GetMapping(value = ["/api/test/pdf/{fnr}/{utbetalingId}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun hentPdf(
        @PathVariable fnr: String,
        @PathVariable utbetalingId: String,
        response: HttpServletResponse,
    ): ByteArray {
        dobbeltsjekkProd()
        val hentPdf = arkivaren.hentPdf(fnr = fnr, id = utbetalingId)
        response.setHeader("x-nais-app-image", hentPdf.versjon)

        return hentPdf.pdf
    }
}
