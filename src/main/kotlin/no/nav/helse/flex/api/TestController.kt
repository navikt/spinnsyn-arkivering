package no.nav.helse.flex.api

import no.nav.helse.flex.client.SpinnsynFrontendArkiveringClient
import no.nav.helse.flex.pdfgenerering.Environment
import no.nav.helse.flex.pdfgenerering.createPDFA
import no.nav.security.token.support.core.api.Unprotected
import org.jsoup.Jsoup
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@Unprotected
@Profile("testcontroller")
class TestController(
    val spinnsynFrontendArkiveringClient: SpinnsynFrontendArkiveringClient
) {

    @ResponseBody
    @GetMapping(value = ["/api/test/html/{fnr}/{utbetalingId}"], produces = [MediaType.TEXT_HTML_VALUE])
    fun hentHtml(@PathVariable fnr: String, @PathVariable utbetalingId: String): String {

        val html =
            spinnsynFrontendArkiveringClient.hentVedtakSomHtml(utbetalingId = utbetalingId, fnr = fnr)

        val doc = Jsoup.parse(html)
        doc.select("div.rc").forEachIndexed { index, element ->
            val titleAnchor = element.select("h3 a")
            val title = titleAnchor.text()
            val url = titleAnchor.attr("href")
            println("$index. $title ($url)")
        }
        return html
    }

    @ResponseBody
    @GetMapping(value = ["/api/test/pdf/{fnr}/{utbetalingId}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun hentPdf(@PathVariable fnr: String, @PathVariable utbetalingId: String): ByteArray {
        val html = spinnsynFrontendArkiveringClient
            .hentVedtakSomHtml(utbetalingId = utbetalingId, fnr = fnr)

        return createPDFA(html, Environment())
    }
}
