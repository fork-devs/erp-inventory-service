package com.erp.inventory.infrastructure.config;

import com.erp.inventory.application.service.BranchService;
import com.erp.inventory.application.service.OutboxProcessorService;
import com.erp.inventory.application.service.SparePartService;
import com.erp.inventory.application.service.StockOperationService;
import com.erp.inventory.domain.port.BranchRepositoryPort;
import com.erp.inventory.domain.port.SparePartRepositoryPort;
import com.erp.inventory.domain.port.EventPublisherPort;
import com.erp.inventory.domain.port.StockLotRepositoryPort;
import com.erp.inventory.domain.port.StockOutboxRepositoryPort;
import com.erp.inventory.domain.port.StockMovementRepositoryPort;
import com.erp.inventory.domain.port.UnitOfWork;
import com.erp.inventory.infrastructure.messaging.DomainEventSerializer;
import com.erp.inventory.infrastructure.security.JwksKeyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
public class ApplicationServiceConfig {

    @Bean
    public BranchService branchService(BranchRepositoryPort branchRepo, UnitOfWork unitOfWork) {
        return new BranchService(branchRepo, unitOfWork);
    }

    @Bean
    public SparePartService sparePartService(SparePartRepositoryPort sparePartRepo, UnitOfWork unitOfWork) {
        return new SparePartService(sparePartRepo, unitOfWork);
    }

    @Bean
    public StockOperationService stockOperationService(StockMovementRepositoryPort movementRepo,
                                                       StockLotRepositoryPort lotRepo,
                                                       SparePartRepositoryPort sparePartRepo,
                                                       BranchRepositoryPort branchRepo,
                                                       StockOutboxRepositoryPort outboxRepo,
                                                       DomainEventSerializer eventSerializer,
                                                       UnitOfWork unitOfWork) {
        return new StockOperationService(movementRepo, lotRepo, sparePartRepo, branchRepo,
                outboxRepo, eventSerializer, unitOfWork);
    }

    /**
     * Outbox processor service bean: publishes pending messages with exponential
     * backoff retries and dead-letter handling.
     */
    @Bean
    public OutboxProcessorService outboxProcessorService(StockOutboxRepositoryPort outboxRepositoryPort,
                                                         EventPublisherPort eventPublisherPort,
                                                         UnitOfWork unitOfWork,
                                                         @Value("${erp.stock.outbox.max-attempts:7}") int maxAttempts,
                                                         @Value("${erp.stock.outbox.max-delay:PT24H}") java.time.Duration maxDelay) {
        return new OutboxProcessorService(outboxRepositoryPort, eventPublisherPort, unitOfWork, maxAttempts, maxDelay);
    }

    /**
     * JWKS key manager bean for the identity provider.
     *
     * <p>The URL must use an encrypted scheme outside local development profiles:
     * fetching keys over plaintext allows man-in-the-middle key substitution
     * and illegitimate token forging.
     */
    @Bean
    public JwksKeyManager jwksKeyManager(
            @Value("${erp.auth.jwks-url}") String jwksUrl,
            Environment environment) {
        return new JwksKeyManager(jwksUrl, Arrays.asList(environment.getActiveProfiles()));
    }
}
