package com.punestartup.ingestion.messaging;

import com.punestartup.ingestion.event.ScrapeSourceRequestedCommand;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class IngestionCommandPublisher {

    private final RabbitTemplate rabbitTemplate;

    public IngestionCommandPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void requestSourceScrape(ScrapeSourceRequestedCommand command) {
        rabbitTemplate.convertAndSend(
                IngestionMessaging.EXCHANGE,
                IngestionMessaging.SCRAPE_REQUESTED_ROUTING_KEY,
                command);
    }
}
