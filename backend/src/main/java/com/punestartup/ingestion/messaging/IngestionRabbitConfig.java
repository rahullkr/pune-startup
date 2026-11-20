package com.punestartup.ingestion.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IngestionRabbitConfig {

    @Bean
    TopicExchange ingestionExchange() {
        return new TopicExchange(IngestionMessaging.EXCHANGE, true, false);
    }

    @Bean
    DirectExchange ingestionDeadLetterExchange() {
        return new DirectExchange(IngestionMessaging.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue scrapeRequestedQueue() {
        return durableQueue(IngestionMessaging.SCRAPE_REQUESTED_QUEUE);
    }

    @Bean
    Queue jobDiscoveredQueue() {
        return durableQueue(IngestionMessaging.JOB_DISCOVERED_QUEUE);
    }

    @Bean
    Queue runCompletedQueue() {
        return durableQueue(IngestionMessaging.RUN_COMPLETED_QUEUE);
    }

    @Bean
    Queue ingestionDeadLetterQueue() {
        return QueueBuilder.durable(IngestionMessaging.DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding scrapeRequestedBinding(Queue scrapeRequestedQueue, TopicExchange ingestionExchange) {
        return BindingBuilder.bind(scrapeRequestedQueue)
                .to(ingestionExchange)
                .with(IngestionMessaging.SCRAPE_REQUESTED_ROUTING_KEY);
    }

    @Bean
    Binding jobDiscoveredBinding(Queue jobDiscoveredQueue, TopicExchange ingestionExchange) {
        return BindingBuilder.bind(jobDiscoveredQueue)
                .to(ingestionExchange)
                .with(IngestionMessaging.JOB_DISCOVERED_ROUTING_KEY);
    }

    @Bean
    Binding runCompletedBinding(Queue runCompletedQueue, TopicExchange ingestionExchange) {
        return BindingBuilder.bind(runCompletedQueue)
                .to(ingestionExchange)
                .with(IngestionMessaging.RUN_COMPLETED_ROUTING_KEY);
    }

    @Bean
    Binding deadLetterBinding(Queue ingestionDeadLetterQueue, DirectExchange ingestionDeadLetterExchange) {
        return BindingBuilder.bind(ingestionDeadLetterQueue)
                .to(ingestionDeadLetterExchange)
                .with(IngestionMessaging.DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    MessageConverter ingestionMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    private Queue durableQueue(String name) {
        return QueueBuilder.durable(name)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", IngestionMessaging.DEAD_LETTER_EXCHANGE,
                        "x-dead-letter-routing-key", IngestionMessaging.DEAD_LETTER_ROUTING_KEY))
                .build();
    }
}
