package com.erp.inventory.infrastructure.messaging.outbox;

import com.erp.inventory.application.usecase.ProcessOutboxMessagesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "erp.stock.outbox.relay-enabled", havingValue = "true", matchIfMissing = true)
public class StockOutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockOutboxRelayScheduler.class);

    private final ProcessOutboxMessagesUseCase processOutboxMessagesUseCase;
    private final int batchSize;

    public StockOutboxRelayScheduler(
            ProcessOutboxMessagesUseCase processOutboxMessagesUseCase,
            @Value("${erp.stock.outbox.batch-size:50}") int batchSize) {
        this.processOutboxMessagesUseCase = processOutboxMessagesUseCase;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${erp.stock.outbox.interval-ms:5000}")
    public void scheduleOutboxDispatch() {
        int processed = processOutboxMessagesUseCase.processUnpublishedMessages(batchSize);
        if (processed > 0) {
            log.debug("[OUTBOX-RELAY] Se procesaron y despacharon {} eventos de outbox", processed);
        }
    }
}
