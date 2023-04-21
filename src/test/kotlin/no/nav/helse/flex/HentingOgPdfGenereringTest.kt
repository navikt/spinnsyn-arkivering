package no.nav.helse.flex

import no.nav.helse.flex.arkivering.PdfSkaperen
import no.nav.helse.flex.html.HtmlInliner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*

class HentingOgPdfGenereringTest : Testoppsett() {

    @Autowired
    lateinit var arkivaren: PdfSkaperen

    @Autowired
    lateinit var htmlInliner: HtmlInliner

    val uuid = "331c9cf7-b2b4-4c60-938d-00ac3969666b"
    val fnr = "13068712345"

    @Test
    fun henterHtml() {
        htmlInliner.clock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.of("Europe/Oslo"))
        enqueFiler()

        val html = arkivaren.hentSomHtmlOgInlineTing(fnr, uuid)
        val forventetHtml = HentingOgPdfGenereringTest::class.java.getResource("/forventet.html").readText()
        html.html `should be equal to ignoring whitespace` forventetHtml
        validerRequests(uuid, fnr)
    }

    @Test
    fun henterPdf() {
        enqueFiler()

        File("pdf-tests").mkdirs()
        val pdf = arkivaren.hentPdf(fnr, uuid)
        File("pdf-tests/" + OffsetDateTime.now().toString() + ".pdf").writeBytes(pdf.pdf)

        validerRequests(uuid, fnr)
    }
}
