package no.nav.helse.flex.arkivering

import no.nav.helse.flex.client.DokArkivClient
import no.nav.helse.flex.kafka.VedtakStatus
import no.nav.helse.flex.kafka.VedtakStatusDto
import no.nav.helse.flex.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.format.DateTimeFormatter

@Component
class Arkivaren(
    val pdfSkaperen: PdfSkaperen,
    val dokArkivClient: DokArkivClient,
    val arkivertVedtakRepository: ArkivertVedtakRepository,
    @Value("\${nais.app.image}")
    val naisAppImage: String,
) {
    private val log = logger()

    val norskDato: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun arkiverVedtak(vedtak: VedtakStatusDto) {
        if (vedtak.vedtakStatus != VedtakStatus.MOTATT) {
            return
        }
        if (arkivertVedtakRepository.existsByVedtakId(vedtak.id)) {
            log.info("Vedtak med vedtakId: ${vedtak.id} er allerede arkivert.")
            return
        }
        lagreJournalpost(fnr = vedtak.fnr, vedtakId = vedtak.id)
    }

    private fun lagreJournalpost(
        fnr: String,
        vedtakId: String,
    ) {
        val vedtaket = pdfSkaperen.hentPdf(fnr = fnr, id = vedtakId)

        val tittel =
            "Svar på søknad om sykepenger for periode: ${vedtaket.fom.format(norskDato)} " +
                "til ${vedtaket.tom.format(norskDato)}"
        val journalpostRequest = skapJournalpostRequest(fnr, vedtakId, vedtaket.pdf, tittel)
        val journalpostResponse = dokArkivClient.opprettJournalpost(journalpostRequest, vedtakId)

        // TODO: Skal vi kaste en exception her i stedet, eller bare logge melding i tillegg?
        if (!journalpostResponse.journalpostferdigstilt) {
            log.warn("Journalpost ${journalpostResponse.journalpostId} for vedtak $vedtakId ble ikke ferdigstilt.")
        }

        arkivertVedtakRepository.save(
            ArkivertVedtak(
                id = null,
                fnr = fnr,
                vedtakId = vedtakId,
                journalpostId = journalpostResponse.journalpostId,
                opprettet = Instant.now(),
                spinnsynArkiveringImage = naisAppImage,
                spinnsynFrontendImage = vedtaket.versjon,
            ),
        )
        log.info("Arkiverte vedtak med vedtakId: $vedtakId.")
    }
}
