package no.nav.helse.flex.api

import no.nav.helse.flex.arkivering.Arkivaren
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@Unprotected
class DebugAntallVedtakController(
    val arkivaren: Arkivaren,
) {
    @ResponseBody
    @GetMapping(value = ["/api/debug/vedtak/"])
    fun debugVedtak(): Debug {
        val antallVedtak = arkivaren.hentAntallVedtak()
        return Debug(
            antallVedtak = antallVedtak.first,
            manglendeVedtak = antallVedtak.second,
        )
    }
}

data class Debug(
    val antallVedtak: Int,
    val manglendeVedtak: Int,
)
