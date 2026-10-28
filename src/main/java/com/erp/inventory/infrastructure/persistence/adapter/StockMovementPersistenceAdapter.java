package com.erp.inventory.infrastructure.persistence.adapter;

import com.erp.inventory.domain.model.StockMovement;
import com.erp.inventory.domain.port.StockMovementRepositoryPort;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;
import com.erp.inventory.infrastructure.persistence.mapper.StockMovementPersistenceMapper;
import com.erp.inventory.infrastructure.persistence.repository.SpringDataStockMovementRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class StockMovementPersistenceAdapter implements StockMovementRepositoryPort {

    private final SpringDataStockMovementRepository repo;
    private final JdbcTemplate jdbcTemplate;

    public StockMovementPersistenceAdapter(SpringDataStockMovementRepository repo, DataSource dataSource) {
        this.repo = repo;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public StockMovement save(StockMovement movement) {
        return StockMovementPersistenceMapper.toDomain(repo.save(StockMovementPersistenceMapper.toEntity(movement)));
    }

    /**
     * Claims the idempotency key directly via the movement insertion.
     *
     * <p>The INSERT uses {@code ON CONFLICT DO NOTHING}: if the key was already claimed,
     * PostgreSQL rejects the insert **without aborting the active transaction**. Handling
     * uniqueness violations through caught SQL exceptions would abort the PostgreSQL transaction,
     * preventing the losing request from reading the existing record.
     */
    @Override
    public Optional<StockMovement> claim(StockMovement movement) {
        int insertados = jdbcTemplate.update("""
                INSERT INTO stock_movements
                    (id, tenant_id, spare_part_id, branch_id, movement_type, quantity,
                     reference_id, idempotency_key, unit_cost, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, idempotency_key) DO NOTHING
                """,
                movement.getId().getValue(),
                movement.getTenantId().getValue(),
                movement.getSparePartId().getValue(),
                movement.getBranchId().getValue(),
                movement.getMovementType().name(),
                movement.getQuantity().getValue(),
                movement.getReferenceId(),
                movement.getIdempotencyKey() == null ? null : movement.getIdempotencyKey().getValue(),
                movement.getUnitCost(),
                java.sql.Timestamp.from(movement.getCreatedAt()));

        if (insertados == 1) {
            // The claim is newly inserted.
            return Optional.empty();
        }

        return repo.findByIdempotencyKeyAndTenantId(movement.getIdempotencyKey().getValue(),
                        movement.getTenantId().getValue())
                .map(StockMovementPersistenceMapper::toDomain);
    }

    @Override
    public List<StockMovement> findBySparePartAndBranch(SparePartId sparePartId, BranchId branchId, TenantId tenantId) {
        return repo.findBySparePartIdAndBranchIdAndTenantId(sparePartId.getValue(), branchId.getValue(), tenantId.getValue())
                .stream().map(StockMovementPersistenceMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<StockMovement> findBySparePart(SparePartId sparePartId, TenantId tenantId) {
        return repo.findBySparePartIdAndTenantId(sparePartId.getValue(), tenantId.getValue())
                .stream().map(StockMovementPersistenceMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<StockMovement> findByIdempotencyKey(String idempotencyKey, TenantId tenantId) {
        return repo.findByIdempotencyKeyAndTenantId(idempotencyKey, tenantId.getValue())
                .map(StockMovementPersistenceMapper::toDomain);
    }
}
