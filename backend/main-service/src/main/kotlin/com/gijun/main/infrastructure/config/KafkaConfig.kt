package com.gijun.main.infrastructure.config

import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate

@Configuration
class KafkaConfig(
    @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
) {

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        val config = mapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        )
        return KafkaTemplate(DefaultKafkaProducerFactory(config))
    }

    @Bean
    fun matchEventsTopic(): NewTopic =
        TopicBuilder.name("lol.match.events")
            .partitions(3)
            .replicas(1)
            .build()

    @Bean
    fun statsRebuildTopic(): NewTopic =
        TopicBuilder.name("lol.stats.rebuild")
            .partitions(1)
            .replicas(1)
            .build()
}
