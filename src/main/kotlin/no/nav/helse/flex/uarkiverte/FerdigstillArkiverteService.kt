package no.nav.helse.flex.uarkiverte

import no.nav.helse.flex.arkivering.ArkivertVedtakRepository
import no.nav.helse.flex.client.DokArkivClient
import no.nav.helse.flex.client.FerdigstillJournalpostRequest
import no.nav.helse.flex.kafka.ArkivertVedtakDto
import org.springframework.stereotype.Service
 import java.time.LocalDate

@Service
class FerdigstillArkiverteService(
    private val dokArkivClient: DokArkivClient,
    private val arkivertRepository: ArkivertVedtakRepository,
    private val spinnsynClient: SpinnsynBackendRestClient,
) {

    fun ferdigstillArkivertVedtak(vedtakDto: ArkivertVedtakDto) {
        val journalPostId = hentJournalPostId(vedtakDto.id)
        val datoJournal = hentOpprettetDato(vedtakDto.id, vedtakDto.id)
        val vedtakId = vedtakDto.id

        val request = FerdigstillJournalpostRequest(
            journalfoerendeEnhet = "9999",
            journalpostId = journalPostId,
            datoJournal = datoJournal
        )
        dokArkivClient.ferdigstillJournalpost(request = request, vedtakId = vedtakId)
    }

    private fun hentJournalPostId(id: String): String {
        return arkivertRepository.getByVedtakId(id).journalpostId
    }

    private fun hentOpprettetDato(fnr: String, id: String): LocalDate {
        return spinnsynClient.hentVedtak(fnr).first { vedtak -> vedtak.id == id }.opprettet
    }
}
