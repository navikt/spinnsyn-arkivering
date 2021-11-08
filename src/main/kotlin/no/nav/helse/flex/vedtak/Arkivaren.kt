package no.nav.helse.flex.vedtak

import no.nav.helse.flex.client.SpinnsynFrontendArkiveringClient
import no.nav.helse.flex.html.HtmlInliner
import no.nav.helse.flex.kafka.Vedtak
import no.nav.helse.flex.pdfgenerering.PdfGenerering.createPDFA
import org.springframework.stereotype.Component

@Component
class Arkivaren(
    val spinnsynFrontendArkiveringClient: SpinnsynFrontendArkiveringClient,
    val htmlInliner: HtmlInliner,
) {
    fun hentSomHtmlOgInlineTing(fnr: String, utbetalingId: String): Pair<String, String> {
        val html = spinnsynFrontendArkiveringClient.hentVedtakSomHtml(utbetalingId = utbetalingId, fnr = fnr)
        return Pair(htmlInliner.inlineHtml(html.html), html.versjon)
    }

    fun hentPdf(fnr: String, utbetalingId: String): Pair<ByteArray, String> {

        val html = hentSomHtmlOgInlineTing(fnr, utbetalingId)

        return Pair(hentPdfFraHtml(html.first), html.second)
    }

    fun hentPdfFraHtml(html: String): ByteArray {
        val nyDoctype = """
            <!DOCTYPE html PUBLIC
 "-//OPENHTMLTOPDF//DOC XHTML Character Entities Only 1.0//EN" "">
        """.trimIndent()

        return createPDFA(html.replaceFirst("<!DOCTYPE html>", nyDoctype))
    }

    fun arkiverVedtak(vedtak: Vedtak) {
        hentPdf(fnr = vedtak.fnr, utbetalingId = vedtak.id)
    }
}
