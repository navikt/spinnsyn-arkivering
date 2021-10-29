package no.nav.helse.flex.api

import no.nav.helse.flex.logger
import no.nav.helse.flex.pdfgenerering.Environment
import no.nav.helse.flex.pdfgenerering.createPDFA
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@Profile("testcontroller")
class TestController {

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
    @GetMapping(value = ["/api/test-html/"], produces = [MediaType.TEXT_HTML_VALUE])
    fun hentHTml(): String {

        return html
    }

    @ResponseBody
    @GetMapping(value = ["/api/test-pdf/"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun hentPdf(): ByteArray {

        return createPDFA(html, Environment())
    }
}
