package no.nav.helse.flex.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.arkivering.Arkivaren
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class VedtakStatusListener(
    val arkivaren: Arkivaren,
    private val environment: EnvironmentToggles,
) {
    val log = logger()

    @KafkaListener(
        topics = [FLEX_VEDTAK_STATUS_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        groupId = "spinnsyn-arkivering-tell-vedtak-v1",
        properties = [
            "auto.offset.reset=earliest",
            "max.poll.records=1000",
        ],
    )
    fun debug(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        val vedtak = cr.value().tilVedtak()
        arkivaren.tellManglendeVedtak(vedtak)
        acknowledgment.acknowledge()
    }

    @KafkaListener(
        topics = [FLEX_VEDTAK_STATUS_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        val vedtak = cr.value().tilVedtak()
        try {
            arkivaren.arkiverVedtak(vedtak)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            if (!environment.isProduction()) {
                if (e.message?.contains("Fant ikke aktørid for person i pdl") == true) {
                    log.info("Fant ikke aktørid for person i pdl, ignorerer melding siden vi er i dev " + vedtak.id)
                    acknowledgment.acknowledge()
                    return
                }
            }
            throw e
        }
    }

    fun String.tilVedtak(): VedtakStatusDto = objectMapper.readValue(this)
}

const val FLEX_VEDTAK_STATUS_TOPIC = "flex.vedtak-status"

data class VedtakStatusDto(
    val id: String,
    val fnr: String,
    val vedtakStatus: VedtakStatus,
)

enum class VedtakStatus {
    MOTATT,
    LEST,
}
