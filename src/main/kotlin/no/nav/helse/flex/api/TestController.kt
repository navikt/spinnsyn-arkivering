package no.nav.helse.flex.api

import no.nav.helse.flex.client.SpinnsynFrontendArkiveringClient
import no.nav.helse.flex.pdfgenerering.Environment
import no.nav.helse.flex.pdfgenerering.createPDFA
import no.nav.security.token.support.core.api.Unprotected
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import java.net.URL
import java.util.*

@Controller
@Unprotected
@Profile("testcontroller")
class TestController(
    val spinnsynFrontendArkiveringClient: SpinnsynFrontendArkiveringClient,
    @Value("\${spinnsyn.frontend.arkivering.url}") private val url: String,
) {

    fun hentSomHtmlOgInlineTing(fnr: String, utbetalingId: String): String {
        val html =
            spinnsynFrontendArkiveringClient.hentVedtakSomHtml(utbetalingId = utbetalingId, fnr = fnr)
        val doc = Jsoup.parse(html)
        doc.select("link").forEach {
            if (it.hasAttr("href")) {
                val stylesheet = URL(url + it.attr("href")).readText()
                it.parent().append("<style>\n$stylesheet\n</style>")
                it.remove()
            } else {
                throw RuntimeException("Style uten href")
            }
        }
        val tvungenFontStyle = """
    <style>
        * {
            font-family: "Source Sans Pro" !important;
        }
    </style>            
        """
        doc.select("head").forEach {
            it.append(tvungenFontStyle)
        }
        doc.select("script").forEach {
            it.remove()
        }
        doc.select("meta").forEach {
            it.remove()
        }
        doc.select("svg").forEach {
            if (!it.hasAttr("xmlns")) {
                it.attr("xmlns", "http://www.w3.org/2000/svg")
            }
        }
        doc.select("img").forEach {
            if (it.hasAttr("src")) {
                val img = URL(url + it.attr("src")).readBytes()
                val b64img = Base64.getEncoder().encodeToString(img) // TODO må verifisere at det er svg og at svg har xmlns
                it.removeAttr("src")
                it.attr("src", "data:image/svg+xml;base64,$b64img")
            }
            it.remove()
        }
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml)

        return doc.toString()
    }

    @ResponseBody
    @GetMapping(value = ["/api/test/html/{fnr}/{utbetalingId}"], produces = [MediaType.TEXT_HTML_VALUE])
    fun hentHtml(@PathVariable fnr: String, @PathVariable utbetalingId: String): String {

        return hentSomHtmlOgInlineTing(fnr, utbetalingId)
    }

    @ResponseBody
    @GetMapping(value = ["/api/test/pdf/{fnr}/{utbetalingId}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun hentPdf(@PathVariable fnr: String, @PathVariable utbetalingId: String): ByteArray {
        val html = hentSomHtmlOgInlineTing(fnr, utbetalingId).replaceFirst("<!doctype html>", "")

        val medDoctype = """
            <!DOCTYPE html PUBLIC
 "-//OPENHTMLTOPDF//DOC XHTML Character Entities Only 1.0//EN" "">
 $html
        """.trimIndent()

        return createPDFA(medDoctype, Environment())
    }
}
