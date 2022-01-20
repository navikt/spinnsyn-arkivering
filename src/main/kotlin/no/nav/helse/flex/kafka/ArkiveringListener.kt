package no.nav.helse.flex.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class ArkiveringListener() {

    private val log = logger()

    @KafkaListener(
        topics = [FLEX_VEDTAK_ARKIVERING_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
        groupId = "spinnsyn-arkivering-test-ferdigstilling"
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        val vedtak = cr.value().tilUarkivertVedtak()
        log.info(
            "Lest vedtak med offset: ${cr.offset()} og id: ${vedtak.id} fra " +
                "topic: $FLEX_VEDTAK_ARKIVERING_TOPIC og partisjon: ${cr.partition()}."
        )
        acknowledgment.acknowledge()
    }

    fun String.tilUarkivertVedtak(): VedtakArkiveringDTO = objectMapper.readValue(this)
}

const val FLEX_VEDTAK_ARKIVERING_TOPIC = "flex.vedtak-arkivering"

data class VedtakArkiveringDTO(
    val fnr: String,
    val id: String,
)
