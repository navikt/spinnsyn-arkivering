package no.nav.helse.flex.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.uarkiverte.FerdigstillArkiverteService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class ArkiveringListener(
    private val ferdigstillArkiverteService: FerdigstillArkiverteService
) {

    private val log = logger()

    @KafkaListener(
        topics = [FLEX_VEDTAK_ARKIVERING_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
        groupId = "spinnsyn-arkivering-ferdigstilling-test"
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        val arkivertVedtakDto = cr.value().tilArkivertVedtakDto()

        val ferdigstillVedtak = setOf(
            "88804146-28ee-30d9-b0a3-a904919c7a37",
            "557080c1-5c9d-31d9-a549-153d6d3f31bb",
            "9b6ed755-aa9a-3463-b3fa-3f70e0a820e1"
        )

        if (ferdigstillVedtak.contains(arkivertVedtakDto.id)) {
            log.info(
                "Lest vedtak med id: ${arkivertVedtakDto.id} fra topic: $FLEX_VEDTAK_ARKIVERING_TOPIC, " +
                    "partisjon: ${cr.partition()} og offset: ${cr.offset()}."
            )
            ferdigstillArkiverteService.ferdigstillVedtak(arkivertVedtakDto)
        }
        acknowledgment.acknowledge()
    }

    fun String.tilArkivertVedtakDto(): ArkivertVedtakDto = objectMapper.readValue(this)
}

const val FLEX_VEDTAK_ARKIVERING_TOPIC = "flex.vedtak-arkivering"

data class ArkivertVedtakDto(
    val fnr: String,
    val id: String,
)
