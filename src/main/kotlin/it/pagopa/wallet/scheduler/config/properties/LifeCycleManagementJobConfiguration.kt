package it.pagopa.wallet.scheduler.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("lifecycle.job.execution")
class LifeCycleManagementJobConfiguration(val limit: Int)