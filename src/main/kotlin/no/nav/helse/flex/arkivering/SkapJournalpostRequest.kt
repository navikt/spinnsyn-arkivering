package no.nav.helse.flex.arkivering

import no.nav.helse.flex.client.domain.AvsenderMottaker
import no.nav.helse.flex.client.domain.Bruker
import no.nav.helse.flex.client.domain.Dokument
import no.nav.helse.flex.client.domain.Dokumentvarianter
import no.nav.helse.flex.client.domain.JournalpostRequest
import no.nav.helse.flex.client.domain.Sak

fun skapJournalpostRequest(
    fnr: String,
    id: String,
    pdf: ByteArray,
    tittel: String,
): JournalpostRequest =
    JournalpostRequest(
        bruker =
            Bruker(
                id = fnr,
                idType = "FNR",
            ),
        dokumenter =
            listOf(
                Dokument(
                    dokumentvarianter =
                        listOf(
                            Dokumentvarianter(
                                filnavn = tittel,
                                filtype = "PDFA",
                                variantformat = "ARKIV",
                                fysiskDokument = pdf,
                            ),
                        ),
                    tittel = tittel,
                ),
            ),
        sak =
            Sak(
                sakstype = "GENERELL_SAK",
            ),
        kanal = "NAV_NO_UTEN_VARSLING",
        journalpostType = "UTGAAENDE",
        journalfoerendeEnhet = "9999",
        eksternReferanseId = id,
        tema = "SYK",
        tittel = tittel,
        avsenderMottaker =
            AvsenderMottaker(
                id = fnr,
                idType = "FNR",
            ),
    )
