package no.nav.helse.flex.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.arkivering.Arkivaren
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class VedtakStatusListener(
    val arkivaren: Arkivaren
) {

    val log = logger()

    @KafkaListener(
        topics = [FLEX_VEDTAK_STATUS_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory"
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        try {
            val vedtak = cr.value().tilVedtak()
            arkivaren.arkiverVedtak(vedtak)
        } catch (e: Exception) {
            log.error("Feil ved arkivering av vedtak", e)
        } finally {
            acknowledgment.acknowledge()
        }
    }

    fun String.tilVedtak(): VedtakStatusDto = objectMapper.readValue(this)
}

const val FLEX_VEDTAK_STATUS_TOPIC = "flex.vedtak-status"

data class VedtakStatusDto(
    val id: String,
    val fnr: String,
    val vedtakStatus: VedtakStatus
)

enum class VedtakStatus {
    MOTATT,
    LEST
}
