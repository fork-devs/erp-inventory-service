package com.erp.inventory.infrastructure.persistence.mapper;

import com.erp.inventory.domain.model.StockLot;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.StockLotId;
import com.erp.inventory.domain.valueobject.TenantId;
import com.erp.inventory.infrastructure.persistence.entity.StockLotJpaEntity;

import java.time.Instant;

public class StockLotPersistenceMapper {

    private StockLotPersistenceMapper() {
    }

    public static StockLotJpaEntity toEntity(StockLot lot) {
        return new StockLotJpaEntity(
                lot.getId().getValue(),
                lot.getTenantId().getValue(),
                lot.getSparePartId().getValue(),
                lot.getBranchId().getValue(),
                lot.getQuantity(),
                lot.getReservedQuantity(),
                lot.getUnitCost(),
                lot.getEntryDate(),
                Instant.now()
        );
    }

    public static StockLot toDomain(StockLotJpaEntity e) {
        return new StockLot(
                new StockLotId(e.getId()),
                new TenantId(e.getTenantId()),
                new SparePartId(e.getSparePartId()),
                new BranchId(e.getBranchId()),
                e.getQuantity(),
                e.getReservedQuantity(),
                e.getUnitCost(),
                e.getEntryDate()
        );
    }
}
