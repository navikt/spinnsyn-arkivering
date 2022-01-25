package no.nav.helse.flex

import no.nav.helse.flex.kafka.ArkivertVedtakDto
import no.nav.helse.flex.kafka.FLEX_VEDTAK_ARKIVERING_TOPIC
import no.nav.helse.flex.uarkiverte.SpinnsynBackendRestClient
import no.nav.helse.flex.uarkiverte.SpinnsynBackendRestClient.RSVedtakWrapper
import okhttp3.mockwebserver.MockResponse
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldStartWith
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit

class FerdigstillUarkiverteTest() : Testoppsett() {

    @Autowired
    lateinit var kafkaProducer: KafkaProducer<String, String>

    private val vedtakId = UUID.randomUUID().toString()
    private val fnr = "12345678987"

    @Test
    fun `Arkivert vedtak blir ferdigstilt`() {
        val vedtakId = "vedtak-1"
        val fnr = "fnr-1"
        val journalpostId = "journalpost-1"

        opprettArkivertVedtak(vedtakId, fnr, journalpostId)

        val spinnsynBackendResponse = listOf(RSVedtakWrapper(id = vedtakId, opprettet = LocalDate.now()))

        val mockResponse = MockResponse().setBody(spinnsynBackendResponse.serialisertTilString())
        spinnsynBackendMockWebServer.enqueue(mockResponse)

        kafkaProducer.send(
            ProducerRecord(
                FLEX_VEDTAK_ARKIVERING_TOPIC,
                null,
                fnr,
                ArkivertVedtakDto(fnr = fnr, id = vedtakId).serialisertTilString()
            )
        ).get()

        val htmlRequest = spinnsynBackendMockWebServer.takeRequest()

        htmlRequest.path `should be equal to` "/api/v1/arkivering/vedtak"
        htmlRequest.headers["fnr"] `should be equal to` fnr
        htmlRequest.headers["Authorization"]!!.shouldStartWith("Bearer ey")

        await().atMost(10, TimeUnit.SECONDS)

        // TODO: 2 - Verifiser at Arkivet blir kallt
    }

    private fun opprettArkivertVedtak(vedtakId: String, fnr: String, journalpostId: String) {
        namedParameterJdbcTemplate.update(
            """
            INSERT INTO arkivert_vedtak (id, vedtak_id, fnr, journalpost_id, opprettet, spinnsyn_frontend_image, spinnsyn_arkivering_image) 
            VALUES (:id, :vedtak_id, :fnr, :journalpost_id, :opprettet, :frontend_image, :arkivering_image)
        """,
            MapSqlParameterSource()
                .addValue("id", UUID.randomUUID().toString())
                .addValue("vedtak_id", vedtakId)
                .addValue("fnr", fnr)
                .addValue("journalpost_id", journalpostId)
                .addValue("opprettet", Timestamp.from(Instant.now()))
                .addValue("frontend_image", "frontend_image")
                .addValue("arkivering_image", "arkivering_image")
        )
    }
}
