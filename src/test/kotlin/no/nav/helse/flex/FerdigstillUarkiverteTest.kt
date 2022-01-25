package no.nav.helse.flex

import no.nav.helse.flex.kafka.ArkivertVedtakDto
import no.nav.helse.flex.kafka.FLEX_VEDTAK_ARKIVERING_TOPIC
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.sql.Timestamp
import java.time.Instant
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

        kafkaProducer.send(
            ProducerRecord(
                FLEX_VEDTAK_ARKIVERING_TOPIC,
                null,
                fnr,
                ArkivertVedtakDto(fnr = fnr, id = vedtakId).serialisertTilString()
            )
        ).get()

        // TODO: 1 - Verifisert at spinnsyn-backend blirt spurt om vedtakene. Returner liste med vedtak basert på fnr.
        // TODO: 2 - Verifiser at Arkivet blir kallt

        await().atMost(10, TimeUnit.SECONDS).until {
            arkivertVedtakRepository.existsByVedtakId(vedtakId)
        }
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
