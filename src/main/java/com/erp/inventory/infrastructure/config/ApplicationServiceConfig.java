package com.erp.inventory.infrastructure.config;

import com.erp.inventory.application.service.BranchService;
import com.erp.inventory.application.service.SparePartService;
import com.erp.inventory.application.service.StockOperationService;
import com.erp.inventory.domain.port.BranchRepositoryPort;
import com.erp.inventory.domain.port.SparePartRepositoryPort;
import com.erp.inventory.domain.port.StockMovementRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationServiceConfig {
    @Bean
    public BranchService branchService(BranchRepositoryPort branchRepo) {
        return new BranchService(branchRepo);
    }

    @Bean
    public SparePartService sparePartService(SparePartRepositoryPort sparePartRepo) {
        return new SparePartService(sparePartRepo);
    }

    @Bean
    public StockOperationService stockOperationService(StockMovementRepositoryPort movementRepo,
                                                       SparePartRepositoryPort sparePartRepo,
                                                       BranchRepositoryPort branchRepo) {
        return new StockOperationService(movementRepo, sparePartRepo, branchRepo);
    }
}
