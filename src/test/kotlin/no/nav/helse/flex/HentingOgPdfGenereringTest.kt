package no.nav.helse.flex

import no.nav.helse.flex.vedtak.Arkivaren
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class HentingOgPdfGenereringTest : Testoppsett() {
    @Autowired
    lateinit var arkivaren: Arkivaren

    @Test
    fun henterHtml() {
        enqueFil("/testside.html")

        val html = arkivaren.hentSomHtmlOgInlineTing("12345", "1234542")
        val forventetHtml = HentingOgPdfGenereringTest::class.java.getResource("/forventet.html").readText()
        html `should be equal to ignoring whitespace` forventetHtml
    }

    fun enqueFil(fil: String) {
        val innhold = HentingOgPdfGenereringTest::class.java.getResource(fil).readText()

        spinnsynArkiveringFrontendMockWebServer.enqueue(MockResponse().setBody(innhold))
    }
}
