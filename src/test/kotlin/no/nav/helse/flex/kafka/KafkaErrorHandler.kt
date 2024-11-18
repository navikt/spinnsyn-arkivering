package no.nav.helse.flex.kafka

import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.stereotype.Component
import org.springframework.util.backoff.FixedBackOff

// Slår av BackOff sånn at det ikke gjøres retries i testene.
@Component
@Profile("test")
@Primary
class KafkaErrorHandler : DefaultErrorHandler(FixedBackOff(0L, 0))
