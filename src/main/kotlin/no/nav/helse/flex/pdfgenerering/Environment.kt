package no.nav.helse.flex.pdfgenerering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.objectMapper
import org.apache.pdfbox.io.IOUtils
import java.nio.file.Path
import java.nio.file.Paths

val fontsRoot: Path = Paths.get("fonts/")

data class Environment(
    val colorProfile: ByteArray = IOUtils.toByteArray(Environment::class.java.getResourceAsStream("/sRGB2014.icc")),
    val fonts: List<FontMetadata> = objectMapper.readValue(Environment::class.java.getResourceAsStream("/fonts/config.json")),
)
