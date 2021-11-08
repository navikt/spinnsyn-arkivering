package no.nav.helse.flex.vedtak

import no.nav.helse.flex.client.DokArkivClient
import no.nav.helse.flex.client.SpinnsynFrontendArkiveringClient
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.html.HtmlInliner
import no.nav.helse.flex.kafka.VedtakStatus
import no.nav.helse.flex.pdfgenerering.PdfGenerering.createPDFA
import org.springframework.stereotype.Component

@Component
class Arkivaren(
    val spinnsynFrontendArkiveringClient: SpinnsynFrontendArkiveringClient,
    val htmlInliner: HtmlInliner,
    val dokArkivClient: DokArkivClient,
    val environmentToggles: EnvironmentToggles
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

    fun arkiverVedtak(vedtak: VedtakStatus) {
        val hentPdf = hentPdf(fnr = vedtak.fnr, utbetalingId = vedtak.id)
        if (environmentToggles.isProduction()) {
            return
        }
        val request = skapJournalpostRequest(vedtak, hentPdf.first)
        dokArkivClient.opprettJournalpost(request, vedtak.id)
    }
}
