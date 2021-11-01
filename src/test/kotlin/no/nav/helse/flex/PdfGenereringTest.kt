package no.nav.helse.flex

import no.nav.helse.flex.pdfgenerering.Environment
import no.nav.helse.flex.pdfgenerering.createPDFA
import org.junit.jupiter.api.Test
import org.verapdf.pdfa.VeraGreenfieldFoundryProvider

class PdfGenereringTest {

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

    @Test
    fun contextLoads() {
        VeraGreenfieldFoundryProvider.initialise()

        createPDFA(html, Environment())
        //  File(Instant.now().epochSecond.toString() + ".pdf").writeBytes(fil)
    }
}
