package no.nav.helse.flex

import no.nav.helse.flex.arkivering.ArkivertVedtakRepository
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldStartWith
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider

private class PostgreSQLContainer14 : PostgreSQLContainer<PostgreSQLContainer14>("postgres:14-alpine")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@EnableMockOAuth2Server
@AutoConfigureMockMvc
abstract class FellesTestOppsett {
    @Autowired
    lateinit var arkivertVedtakRepository: ArkivertVedtakRepository

    companion object {
        var spinnsynArkiveringFrontendMockWebServer: MockWebServer
        var dokarkivMockWebServer: MockWebServer

        init {
            VeraGreenfieldFoundryProvider.initialise()

            PostgreSQLContainer14().apply {
                withCommand("postgres", "-c", "wal_level=logical")
                start()
                System.setProperty("spring.datasource.url", "$jdbcUrl&reWriteBatchedInserts=true")
                System.setProperty("spring.datasource.username", username)
                System.setProperty("spring.datasource.password", password)
            }

            KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.1")).also {
                it.start()
                System.setProperty("KAFKA_BROKERS", it.bootstrapServers)
            }

            spinnsynArkiveringFrontendMockWebServer =
                MockWebServer()
                    .also { it.start() }
                    .also {
                        System.setProperty("spinnsyn.frontend.arkivering.url", "http://localhost:${it.port}")
                    }

            dokarkivMockWebServer =
                MockWebServer()
                    .also { it.start() }
                    .also {
                        System.setProperty("dokarkiv.url", "http://localhost:${it.port}")
                    }
        }
    }

    @AfterAll
    fun `Vi tømmer databasen`() {
        arkivertVedtakRepository.deleteAll()
    }

    fun enqueFiler() {
        enqueFil("/testside.html", imageName = "docker.github/spinnsyn-frontend-v2.0", fom = "2020-03-12", tom = "2020-04-30")
        enqueFil("/stylesheet.css")
    }

    fun enqueFil(
        fil: String,
        imageName: String? = null,
        fom: String? = null,
        tom: String? = null,
    ) {
        val innhold = HentingOgPdfGenereringTest::class.java.getResource(fil).readText()

        val response = MockResponse().setBody(innhold)
        imageName?.let {
            response.setHeader("x-nais-app-image", it)
        }
        tom?.let {
            response.setHeader("x-vedtak-tom", it)
        }
        fom?.let {
            response.setHeader("x-vedtak-fom", it)
        }

        spinnsynArkiveringFrontendMockWebServer.enqueue(response)
    }

    fun validerRequests(
        uuid: String,
        fnr: String,
    ) {
        val htmlRequest = spinnsynArkiveringFrontendMockWebServer.takeRequest()
        htmlRequest.path `should be equal to` "/syk/sykepenger/vedtak/arkivering/$uuid"
        htmlRequest.headers["fnr"] `should be equal to` fnr
        htmlRequest.headers["Authorization"]!!.shouldStartWith("Bearer ey")

        val stylesheetRequest = spinnsynArkiveringFrontendMockWebServer.takeRequest()
        stylesheetRequest.path `should be equal to` "/syk/sykepenger/_next/static/css/yes.css"
        stylesheetRequest.headers["fnr"].shouldBeNull()
        stylesheetRequest.headers["Authorization"].shouldBeNull()
    }
}
