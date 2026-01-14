package no.nav.helse.flex.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.arkivering.Arkivaren
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Profile("!test")
@Component
class ArkiveringFixVedtakStatusListener(
    val arkivaren: Arkivaren,
    private val environment: EnvironmentToggles,
) : ConsumerSeekAware {
    val log = logger()

    @KafkaListener(
        topics = [FLEX_VEDTAK_STATUS_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        groupId = "flex-arkivering-fix-vedtak-status-2",
        properties = ["auto.offset.reset = earliest"],
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        val tidspunkt = Instant.ofEpochMilli(cr.timestamp()).atOffset(ZoneOffset.UTC)
        if (tidspunkt.isAfter(LocalDate.of(2026, 1, 14).atStartOfDay().atOffset(ZoneOffset.UTC))) {
            log.info("Hopper over vedtak med timestamp $tidspunkt siden det er etter fiks")
            acknowledgment.acknowledge()
            return
        }

        log.info("Offset: ${cr.offset()} Partition: ${cr.partition()} Timestamp: $tidspunkt")

        val vedtak = cr.value().tilVedtak()
        log.info("Mottok vedtak med id: ${vedtak.id} og status: ${vedtak.vedtakStatus}")
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
                log.error("Feilet ved arkivering av vedtak status: ${vedtak.id} i DEV. Hopper over.", e)
                return
            }
            log.error("Feilet ved arkivering av vedtak status: ${vedtak.id}", e)
            throw e
        }
    }

    override fun onPartitionsAssigned(
        assignments: MutableMap<TopicPartition, Long>,
        callback: ConsumerSeekAware.ConsumerSeekCallback,
    ) {
        val startAt =
            LocalDate
                .of(2025, 12, 1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()

        assignments.keys.forEach { topicPartition ->
            callback.seekToTimestamp(topicPartition.topic(), topicPartition.partition(), startAt)
        }
    }

    fun String.tilVedtak(): VedtakStatusDto = objectMapper.readValue(this)
}
