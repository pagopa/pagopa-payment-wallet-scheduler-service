package it.pagopa.wallet.scheduler.config

import it.pagopa.wallet.scheduler.utils.InstantReader
import it.pagopa.wallet.scheduler.utils.InstantWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

@Configuration
class MongoConfiguration {

    @Bean
    fun mongoCustomConversions() = MongoCustomConversions(listOf(InstantReader, InstantWriter))
}
