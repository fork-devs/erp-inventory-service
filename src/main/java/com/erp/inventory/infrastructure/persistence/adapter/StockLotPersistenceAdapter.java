package com.erp.inventory.infrastructure.persistence.adapter;

import com.erp.inventory.domain.model.StockLot;
import com.erp.inventory.domain.port.StockLotRepositoryPort;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;
import com.erp.inventory.infrastructure.persistence.mapper.StockLotPersistenceMapper;
import com.erp.inventory.infrastructure.persistence.repository.SpringDataStockLotRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adaptador de los lotes de stock.
 *
 * <p>No declara demarcacion propia: se une a la transaccion que abre la
 * capa de aplicacion, y el bloqueo de los lotes vive dentro de ella.
 */
@Component
public class StockLotPersistenceAdapter implements StockLotRepositoryPort {

    private final SpringDataStockLotRepository repo;

    public StockLotPersistenceAdapter(SpringDataStockLotRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<StockLot> bloquearLotes(TenantId tenantId, SparePartId sparePartId, BranchId branchId) {
        return repo.bloquearLotes(tenantId.getValue(), sparePartId.getValue(), branchId.getValue())
                .stream()
                .map(StockLotPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public StockLot guardar(StockLot lot) {
        return StockLotPersistenceMapper.toDomain(repo.save(StockLotPersistenceMapper.toEntity(lot)));
    }

    @Override
    public List<StockLot> listar(TenantId tenantId, SparePartId sparePartId, BranchId branchId) {
        return repo.findByTenantIdAndSparePartIdAndBranchIdOrderByEntryDateAsc(
                        tenantId.getValue(), sparePartId.getValue(), branchId.getValue())
                .stream()
                .map(StockLotPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }
}
