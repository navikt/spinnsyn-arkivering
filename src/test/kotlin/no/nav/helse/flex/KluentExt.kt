package no.nav.helse.flex

import io.kotest.matchers.shouldBe

@Suppress("ktlint:standard:function-naming")
infix fun String.`should be equal to ignoring whitespace`(expected: String) =
    this
        .filterNot {
            it.isWhitespace()
        }.shouldBe(expected.filterNot { it.isWhitespace() })
