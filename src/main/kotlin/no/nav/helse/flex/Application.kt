package no.nav.helse.flex

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider

@SpringBootApplication
@EnableRetry
class Application

fun main(args: Array<String>) {
    VeraGreenfieldFoundryProvider.initialise()
    runApplication<Application>(*args)
}

val objectMapper: ObjectMapper =
    ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

fun Any.serialisertTilString(): String = objectMapper.writeValueAsString(this)
