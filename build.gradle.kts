import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
    kotlin("plugin.spring") version "2.1.21"
    kotlin("jvm") version "2.2.0"
}

group = "no.nav.helse.flex"
version = "1.0.0"
description = "spinnsyn-arkivering"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/navikt/maven-release")
    }
}

ext["okhttp3.version"] = "4.12" // Token-support tester trenger MockWebServer.

val testContainersVersion = "1.21.2"
val tokenSupportVersion = "5.0.29"
val logstashLogbackEncoderVersion = "8.1"
val kluentVersion = "1.73"
val openHtmlToPdfVersion = "1.0.10"
val veraPdfVersion = "1.28.1"
val jsoupVersion = "1.20.1"
val jaxbRuntimeVersion = "2.4.0-b180830.0438"

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.hibernate.validator:hibernate-validator")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.apache.httpcomponents.client5:httpclient5")
    implementation("org.aspectj:aspectjrt")
    implementation("org.aspectj:aspectjweaver")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")
    implementation("no.nav.security:token-client-spring:$tokenSupportVersion")
    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("com.openhtmltopdf:openhtmltopdf-pdfbox:$openHtmlToPdfVersion")
    implementation("com.openhtmltopdf:openhtmltopdf-slf4j:$openHtmlToPdfVersion")
    implementation("com.openhtmltopdf:openhtmltopdf-svg-support:$openHtmlToPdfVersion")
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("org.verapdf:validation-model:$veraPdfVersion")
    // veraPDF trenger fortsat javax-pakker: https://github.com/veraPDF/veraPDF-library/issues/1314
    implementation("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")

    testImplementation(platform("org.testcontainers:testcontainers-bom:$testContainersVersion"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.awaitility:awaitility")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
}

ktlint {
    version.set("1.5.0")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
        if (System.getenv("CI") == "true") {
            allWarningsAsErrors.set(true)
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        testLogging {
            events("PASSED", "FAILED", "SKIPPED")
            exceptionFormat = TestExceptionFormat.FULL
        }
        failFast = false
    }
}

tasks {
    bootJar {
        archiveFileName = "app.jar"
    }
}
