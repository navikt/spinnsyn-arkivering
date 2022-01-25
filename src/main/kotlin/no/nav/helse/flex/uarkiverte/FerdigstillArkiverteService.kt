package no.nav.helse.flex.uarkiverte

import no.nav.helse.flex.arkivering.ArkivertVedtakRepository
import no.nav.helse.flex.client.DokArkivClient
import no.nav.helse.flex.client.FerdigstillJournalpostRequest
import no.nav.helse.flex.kafka.ArkivertVedtakDto
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class FerdigstillArkiverteService(
    private val dokArkivClient: DokArkivClient,
    private val arkivertVedtakRepository: ArkivertVedtakRepository,
) {

    fun ferdigstillArkivertVedtak(arkivertVedtakDto: ArkivertVedtakDto) {
        val journalPostId = finnJournalPostId(arkivertVedtakDto.id)
        val datoJournal = hentOpprettetDato(arkivertVedtakDto.id, arkivertVedtakDto.id)
        val vedtakId = arkivertVedtakDto.id

        val request = FerdigstillJournalpostRequest(
            journalfoerendeEnhet = "9999",
            journalpostId = journalPostId,
            datoJournal = datoJournal
        )
        dokArkivClient.ferdigstillJournalpost(request = request, vedtakId = vedtakId)
    }

    private fun finnJournalPostId(id: String): String {
        return arkivertVedtakRepository.getByVedtakId(id).journalpostId
    }

    private fun hentOpprettetDato(fnr: String, id: String): LocalDateTime {
        TODO("Not yet implemented")
    }
}
