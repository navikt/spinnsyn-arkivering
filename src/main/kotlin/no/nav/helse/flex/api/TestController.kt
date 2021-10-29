package no.nav.helse.flex.api

import no.nav.helse.flex.client.SpinnsynFrontendArkiveringClient
import no.nav.helse.flex.logger
import no.nav.helse.flex.pdfgenerering.Environment
import no.nav.helse.flex.pdfgenerering.createPDFA
import no.nav.security.token.support.core.api.Unprotected
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

    private val log = logger()

    val html = "<!DOCTYPE html>\n" +
        "<html>\n" +
        "<head>\n" +
        "    <title>Test template</title>\n" +
        "    <style>\n" +
        "        h1 {\n" +
        "            font-family: \"Source Sans Pro\" !important;\n" +
        "        }\n" +
        "    </style>\n" +
        "</head>\n" +
        "<body>\n" +
        "<h1>This document should render fine</h1>\n" +
        "</body>\n" +
        "</html>\n"

    @ResponseBody
    @GetMapping(value = ["/api/test/html/{fnr}/{utbetalingId}"], produces = [MediaType.TEXT_HTML_VALUE])
    fun hentHtml(@PathVariable fnr: String, @PathVariable utbetalingId: String): String {

        return spinnsynFrontendArkiveringClient.hentVedtakSomHtml(utbetalingId = utbetalingId, fnr = fnr)
    }

    @ResponseBody
    @GetMapping(value = ["/api/test-pdf/"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun hentPdf(): ByteArray {

        return createPDFA(html, Environment())
    }
}
