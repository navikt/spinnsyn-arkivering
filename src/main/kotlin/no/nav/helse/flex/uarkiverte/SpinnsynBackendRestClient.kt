package no.nav.helse.flex.uarkiverte

import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SpinnsynBackendRestClient {

    fun hentVedtak(fnr: String): List<Vedtak> {
        TODO("Not yet implemented")
    }

    private data class RSVedtakWrapper(
        val id: String,
        val opprettet: LocalDate,
    )

    private fun RSVedtakWrapper.tilVedtak(): Vedtak = Vedtak(this.id, this.opprettet)
}

data class Vedtak(
    val id: String,
    val opprettet: LocalDate,
)
