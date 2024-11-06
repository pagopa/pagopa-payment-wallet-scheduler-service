plugins {
  kotlin("jvm") version "1.9.25"
  kotlin("plugin.spring") version "1.9.25"
  id("org.springframework.boot") version "3.3.5"
  id("io.spring.dependency-management") version "1.1.6"
  id("com.diffplug.spotless") version "6.18.0"
  id("org.sonarqube") version "4.0.0.2929"
  id("com.dipien.semantic-version") version "2.0.0" apply false
  jacoco
  application
}

group = "it.pagopa.wallet"

version = "0.0.0"

description = "pagopa-payment-wallet-scheduler-service"

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

repositories {
  mavenCentral()
  mavenLocal()
}

object Dependencies {
  const val ecsLoggingVersion = "1.5.0"
  const val openTelemetryVersion = "1.37.0"
}

dependencyLocking { lockAllConfigurations() }

dependencyManagement {
  imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.5") }
  imports { mavenBom("com.azure.spring:spring-cloud-azure-dependencies:5.13.0") }
  // Kotlin BOM
  imports { mavenBom("org.jetbrains.kotlin:kotlin-bom:1.7.22") }
  imports { mavenBom("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.4") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.redisson:redisson-spring-boot-starter:3.38.1")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("co.elastic.logging:logback-ecs-encoder:${Dependencies.ecsLoggingVersion}")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  // otel api
  implementation("io.opentelemetry:opentelemetry-api:${Dependencies.openTelemetryVersion}")
  // azure storage queue
  implementation("com.azure.spring:spring-cloud-azure-starter")
  implementation("com.azure:azure-storage-queue")
  implementation("com.azure:azure-core-serializer-json-jackson")
  // Byte Buddy
  implementation("net.bytebuddy:byte-buddy:1.15.3")
  // tests
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
  testImplementation("io.netty:netty-resolver-dns-native-macos:4.1.82.Final:osx-aarch_64")
  testImplementation("com.github.codemonstur:embedded-redis:1.4.3")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations {
  implementation.configure {
    exclude(module = "spring-boot-starter-web")
    exclude("org.apache.tomcat")
    exclude(group = "org.slf4j", module = "slf4j-simple")
  }
}

kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }

springBoot {
  mainClass.set("it.pagopa.wallet.scheduler.PagopaPaymentWalletSchedulerServiceApplicationKt")
  buildInfo {
    properties {
      additional.set(mapOf("description" to (project.description ?: "Default description")))
    }
  }
}

tasks.withType<Test> { useJUnitPlatform() }

tasks.named<Jar>("jar") { enabled = false }

tasks.create("applySemanticVersionPlugin") {
  group = "semantic-versioning"
  description = "Semantic versioning plugin"
  dependsOn("prepareKotlinBuildScriptModel")
  apply(plugin = "com.dipien.semantic-version")
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    toggleOffOn()
    targetExclude("build/**/*")
    ktfmt().kotlinlangStyle()
  }
  kotlinGradle {
    toggleOffOn()
    targetExclude("build/**/*.kts")
    ktfmt().googleStyle()
  }
  java {
    target("**/*.java")
    targetExclude("build/**/*")
    eclipse().configFile("eclipse-style.xml")
    toggleOffOn()
    removeUnusedImports()
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
  dependsOn(tasks.test) // tests are required to run before generating the report

  classDirectories.setFrom(
    files(
      classDirectories.files.map {
        fileTree(it).matching {
          exclude("it/pagopa/wallet/PagopaPaymentWalletSchedulerServiceApplicationKt.class")
        }
      }
    )
  )

  reports { xml.required.set(true) }
}

/**
 * Task used to expand application properties with build specific properties such as artifact name
 * and version
 */
tasks.processResources { filesMatching("application.properties") { expand(project.properties) } }
