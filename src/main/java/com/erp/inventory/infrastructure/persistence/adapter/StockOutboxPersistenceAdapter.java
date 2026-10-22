package com.erp.inventory.infrastructure.persistence.adapter;

import com.erp.inventory.domain.model.StockOutboxMessage;
import com.erp.inventory.domain.port.StockOutboxRepositoryPort;
import com.erp.inventory.infrastructure.persistence.entity.StockOutboxJpaEntity;
import com.erp.inventory.infrastructure.persistence.mapper.StockOutboxPersistenceMapper;
import com.erp.inventory.infrastructure.persistence.repository.SpringDataStockOutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import com.erp.inventory.infrastructure.persistence.TransactionGuard;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class StockOutboxPersistenceAdapter implements StockOutboxRepositoryPort {

    private final TransactionGuard transactionGuard;
    private final SpringDataStockOutboxRepository repository;

    public StockOutboxPersistenceAdapter(SpringDataStockOutboxRepository repository,
                                        TransactionGuard transactionGuard) {
        this.repository = repository;
        this.transactionGuard = transactionGuard;
    }

    @Override
    public StockOutboxMessage save(StockOutboxMessage message) {
        transactionGuard.requireActiveTransaction("saveOutboxMessage");
        StockOutboxJpaEntity entity = StockOutboxPersistenceMapper.toEntity(message);
        return StockOutboxPersistenceMapper.toDomain(repository.save(entity));
    }

    @Override
    public List<StockOutboxMessage> findPending(Instant now, int limit) {
        // La reclamacion de la fila necesita una transaccion escribible:
        // SELECT ... FOR UPDATE SKIP LOCKED no se puede ejecutar en una
        // transaccion de solo lectura. La abre el relay, en la capa de
        // aplicacion, y se cierra al salir de esta llamada: la publicacion
        // ocurre despues, con la transaccion ya cerrada.
        transactionGuard.requireActiveTransaction("findPendingOutboxMessages");
        return repository.findPendingForUpdate(now, PageRequest.of(0, limit)).stream()
                .map(StockOutboxPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockOutboxMessage> findDeadLettered(UUID tenantId, int limit) {
        return repository.findDeadLettered(tenantId, PageRequest.of(0, limit)).stream()
                .map(StockOutboxPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public StockOutboxMessage reactivate(UUID id) {
        transactionGuard.requireActiveTransaction("reactivateOutboxMessage");
        StockOutboxJpaEntity entity = repository.findByIdForUpdate(id);
        if (entity == null) {
            return null;
        }
        StockOutboxMessage message = StockOutboxPersistenceMapper.toDomain(entity);
        message.reactivate();
        return StockOutboxPersistenceMapper.toDomain(repository.save(StockOutboxPersistenceMapper.toEntity(message)));
    }
}
