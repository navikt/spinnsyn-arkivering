package no.nav.helse.flex

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.client.domain.JournalpostRequest
import no.nav.helse.flex.client.domain.JournalpostResponse
import no.nav.helse.flex.kafka.FLEX_VEDTAK_STATUS_TOPIC
import no.nav.helse.flex.kafka.VedtakStatus
import no.nav.helse.flex.kafka.VedtakStatusDto
import okhttp3.mockwebserver.MockResponse
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldStartWith
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class IntegrasjonTest : FellesTestOppsett() {
    @Autowired
    lateinit var kafkaProducer: KafkaProducer<String, String>

    val vedtakId = UUID.randomUUID().toString()
    val fnr = "12345678987"

    @Test
    @Order(1)
    fun `Arkiverer vedtak`() {
        enqueFiler()
        val journalpostResponse =
            JournalpostResponse(
                dokumenter = emptyList(),
                journalpostId = "jpostid123",
                journalpostferdigstilt = true,
            )
        val response =
            MockResponse()
                .setBody(journalpostResponse.serialisertTilString())
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        dokarkivMockWebServer.enqueue(response)

        kafkaProducer
            .send(
                ProducerRecord(
                    FLEX_VEDTAK_STATUS_TOPIC,
                    null,
                    fnr,
                    VedtakStatusDto(id = vedtakId, fnr = fnr, vedtakStatus = VedtakStatus.MOTATT).serialisertTilString(),
                ),
            ).get()

        await().atMost(10, TimeUnit.SECONDS).until {
            arkivertVedtakRepository.existsByVedtakId(vedtakId)
        }

        validerRequests(vedtakId, fnr)
        val journalfoeringRequest = dokarkivMockWebServer.takeRequest()
        journalfoeringRequest.path `should be equal to` "/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"
        journalfoeringRequest.headers["Authorization"]!!.shouldStartWith("Bearer ey")
        journalfoeringRequest.headers["Nav-Callid"] `should be equal to` vedtakId

        val jpostRequest: JournalpostRequest =
            objectMapper.readValue(journalfoeringRequest.body.readString(Charset.defaultCharset()))
        jpostRequest.tittel `should be equal to` "Svar på søknad om sykepenger for periode: 12.03.2020 til 30.04.2020"
        jpostRequest.kanal `should be equal to` "NAV_NO_UTEN_VARSLING"

        val arkivertVedtak = arkivertVedtakRepository.findAll().first { it.vedtakId == vedtakId }

        arkivertVedtak.vedtakId `should be equal to` vedtakId
        arkivertVedtak.journalpostId `should be equal to` "jpostid123"
        arkivertVedtak.fnr `should be equal to` fnr
        arkivertVedtak.spinnsynArkiveringImage `should be equal to` "docker.github/spinnsyn-arkivering-v2.0"
        arkivertVedtak.spinnsynFrontendImage `should be equal to` "docker.github/spinnsyn-frontend-v2.0"
    }

    @Test
    @Order(2)
    fun `Arkiverer ikke ett duplikat vedtak`() {
        arkivertVedtakRepository.count() `should be equal to` 1L
        kafkaProducer
            .send(
                ProducerRecord(
                    FLEX_VEDTAK_STATUS_TOPIC,
                    null,
                    fnr,
                    VedtakStatusDto(id = vedtakId, fnr = fnr, vedtakStatus = VedtakStatus.MOTATT).serialisertTilString(),
                ),
            ).get()

        await().during(5, TimeUnit.SECONDS).until {
            arkivertVedtakRepository.count() == 1L
        }
    }

    @Test
    @Order(3)
    fun `Arkiverer ikke vedtak som ikke har status LEST`() {
        arkivertVedtakRepository.count() `should be equal to` 1L
        kafkaProducer
            .send(
                ProducerRecord(
                    FLEX_VEDTAK_STATUS_TOPIC,
                    null,
                    fnr,
                    VedtakStatusDto(id = vedtakId, fnr = fnr, vedtakStatus = VedtakStatus.LEST).serialisertTilString(),
                ),
            ).get()

        await().during(5, TimeUnit.SECONDS).until {
            arkivertVedtakRepository.count() == 1L
        }
    }

    @Test
    @Order(4)
    fun `Vedtak blir ikke lagret når lagring av journalpost feiler`() {
        val vedtakId = UUID.randomUUID().toString()
        val fnr = "01019001010"

        enqueFiler()
        val journalpostResponse =
            JournalpostResponse(
                dokumenter = emptyList(),
                journalpostId = "jpostid234",
                journalpostferdigstilt = false,
                melding = "Feil ved oppretting av journalpost.",
            )
        val response =
            MockResponse()
                .setBody(journalpostResponse.serialisertTilString())
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        dokarkivMockWebServer.enqueue(response)

        kafkaProducer
            .send(
                ProducerRecord(
                    FLEX_VEDTAK_STATUS_TOPIC,
                    null,
                    fnr,
                    VedtakStatusDto(id = vedtakId, fnr = fnr, vedtakStatus = VedtakStatus.MOTATT).serialisertTilString(),
                ),
            ).get()

        // Det skal ikke finnes noen arkiverte vedtak siden journalposten ikke ble opprettet.
        await().during(3, TimeUnit.SECONDS).until { arkivertVedtakRepository.findByFnr(fnr).isEmpty() }
    }
}
