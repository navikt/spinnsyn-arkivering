package no.nav.helse.flex.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component
import org.springframework.util.backoff.ExponentialBackOff
import no.nav.helse.flex.logger as slf4jLogger

@Component
class KafkaErrorHandler :
    DefaultErrorHandler(
        null,
        ExponentialBackOff(1000L, 2.0).also {
            // 2 minutter max retry interval, maks 10 forsøk = ca 4 minutter totalt
            it.maxInterval = 60_000L * 2
            it.maxElapsedTime = 60_000L * 4
        },
    ) {
    // Bruker aliased logger for unngå kollisjon med CommonErrorHandler.logger(): LogAccessor.
    val log = slf4jLogger()

    override fun handleBatch(
        thrownException: Exception,
        records: ConsumerRecords<*, *>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        invokeListener: Runnable,
    ) {
        records.forEach { record ->
            log.warn(
                "Retry-forsøk for record med offset: ${record.offset()}, key: ${record.key()} på topic ${record.topic()}. Exception: ${thrownException::class.simpleName}",
            )
        }
        super.handleBatch(thrownException, records, consumer, container, invokeListener)
    }

    override fun handleRemaining(
        thrownException: java.lang.Exception,
        records: MutableList<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
    ) {
        records.forEach { record ->
            log.error(
                "Feil i prossesseringen av record med offset: ${record.offset()}, key: ${record.key()} på topic ${record.topic()}. Gir opp etter retry-forsøk.",
                thrownException,
            )
        }
        if (records.isEmpty()) {
            log.error("Feil i listener uten noen records", thrownException)
        }
        super.handleRemaining(thrownException, records, consumer, container)
    }
}
