package com.erp.inventory.application.service;

import com.erp.inventory.application.dto.command.RegisterStockMovementCommand;
import com.erp.inventory.application.dto.command.ReleaseStockCommand;
import com.erp.inventory.application.dto.command.ReserveStockCommand;
import com.erp.inventory.application.dto.result.StockBalanceResult;
import com.erp.inventory.application.dto.result.StockMovementResult;
import com.erp.inventory.application.usecase.StockOperationUseCase;
import com.erp.inventory.domain.exception.BranchNotFoundException;
import com.erp.inventory.domain.exception.InsufficientStockException;
import com.erp.inventory.domain.exception.SparePartNotFoundException;
import com.erp.inventory.domain.model.MovementType;
import com.erp.inventory.domain.model.StockLot;
import com.erp.inventory.domain.model.StockMovement;
import com.erp.inventory.domain.port.BranchRepositoryPort;
import com.erp.inventory.domain.port.SparePartRepositoryPort;
import com.erp.inventory.domain.port.StockLotRepositoryPort;
import com.erp.inventory.domain.port.StockMovementRepositoryPort;
import com.erp.inventory.domain.port.UnitOfWork;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.IdempotencyKey;
import com.erp.inventory.domain.valueobject.Quantity;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Operaciones de stock: entrada, salida, reserva, confirmacion y liberacion.
 *
 * <p>Tres reglas gobiernan el servicio:
 *
 * <ol>
 *   <li>La <strong>idempotencia se reclama en la base de datos</strong>: el
 *       registro del movimiento ES la reclamacion de la clave, y la unicidad
 *       arbitra entre las peticiones concurrentes con la misma clave. Entre
 *       consultar la clave y registrar el movimiento hay una ventana en la
 *       que otra peticion pasaria la comprobacion, y las dos registrarian su
 *       movimiento.</li>
 *   <li>El disponible se calcula con los <strong>lotes bloqueados</strong>, en
 *       orden de llegada: dos reservas simultaneas no pueden sobre-reservar,
 *       porque la segunda espera el bloqueo y ve el stock ya descontado.</li>
 *   <li>El saldo se valora con los <strong>lotes</strong>, no con la suma de
 *       los movimientos: el saldo dice cuantas unidades hay, y el valor dice
 *       cuanto valen.</li>
 * </ol>
 */
@Service
public class StockOperationService implements StockOperationUseCase {

    private final StockMovementRepositoryPort movementRepository;
    private final StockLotRepositoryPort lotRepository;
    private final SparePartRepositoryPort sparePartRepository;
    private final BranchRepositoryPort branchRepository;
    private final UnitOfWork unitOfWork;

    public StockOperationService(StockMovementRepositoryPort movementRepository,
                                  StockLotRepositoryPort lotRepository,
                                  SparePartRepositoryPort sparePartRepository,
                                  BranchRepositoryPort branchRepository,
                                  UnitOfWork unitOfWork) {
        this.movementRepository = movementRepository;
        this.lotRepository = lotRepository;
        this.sparePartRepository = sparePartRepository;
        this.branchRepository = branchRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public StockMovementResult registerMovement(RegisterStockMovementCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            SparePartId partId = new SparePartId(cmd.sparePartId());
            BranchId branchId = new BranchId(cmd.branchId());
            validateEntitiesExist(partId, branchId, tenantId);
            MovementType type = MovementType.valueOf(cmd.movementType().toUpperCase());
            Quantity qty = Quantity.of(cmd.quantity());

            List<StockLot> lotes = lotRepository.bloquearLotes(tenantId, partId, branchId);

            if (type == MovementType.IN) {
                // Una entrada ABRE un lote: el coste real con el que se compro
                // es el que valora el stock a partir de ahora.
                BigDecimal coste = cmd.unitCost() != null ? cmd.unitCost() : BigDecimal.ZERO;
                StockLot lote = StockLot.open(tenantId, partId, branchId, qty.getValue(), coste, Instant.now());
                lotRepository.guardar(lote);
                StockMovement movimiento = StockMovement.create(tenantId, partId, branchId, type, qty,
                        cmd.referenceId(), claveDe(cmd.idempotencyKey()), coste);
                return toResult(reclamar(movimiento));
            }

            if (type == MovementType.OUT) {
                int disponible = disponibles(lotes);
                if (disponible < qty.getValue()) {
                    throw new InsufficientStockException("Stock insuficiente para salida. Disponible: "
                            + disponible + ", solicitado: " + qty.getValue());
                }
            }

            StockMovement movimiento = StockMovement.create(tenantId, partId, branchId, type, qty,
                    cmd.referenceId(), claveDe(cmd.idempotencyKey()));
            Optional<StockMovement> yaReclamado = movementRepository.claim(movimiento);
            if (yaReclamado.isPresent()) {
                return toResult(yaReclamado.get());
            }

            if (type == MovementType.OUT) {
                consumir(lotes, qty.getValue());
                guardarLotes(lotes);
            }
            return toResult(movimiento);
        });
    }

    @Override
    public StockMovementResult reserve(ReserveStockCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            SparePartId partId = new SparePartId(cmd.sparePartId());
            BranchId branchId = new BranchId(cmd.branchId());
            validateEntitiesExist(partId, branchId, tenantId);

            // Los lotes se BLOQUEAN: la comprobacion del disponible, con ellos
            // bloqueados, es lo que impide sobre-reservar.
            List<StockLot> lotes = lotRepository.bloquearLotes(tenantId, partId, branchId);

            StockMovement movimiento = StockMovement.create(tenantId, partId, branchId, MovementType.RESERVE,
                    Quantity.of(cmd.quantity()), cmd.workOrderId(), claveDe(cmd.idempotencyKey()));

            // La reclamacion va ANTES de la comprobacion: si la clave ya
            // estaba reclamada, se devuelve el resultado de la otra peticion
            // sin comprobar el saldo ni consumir lotes.
            Optional<StockMovement> yaReclamado = movementRepository.claim(movimiento);
            if (yaReclamado.isPresent()) {
                return toResult(yaReclamado.get());
            }

            int disponible = disponibles(lotes);
            if (disponible < cmd.quantity()) {
                // La transaccion se revierte: el movimiento no queda y la clave
                // queda LIBRE para un reintento. Un fallo no se cachea como
                // resultado definitivo.
                throw new InsufficientStockException("No hay suficiente stock disponible para reservar. Disponible: "
                        + disponible + ", Requerido: " + cmd.quantity());
            }

            reservar(lotes, cmd.quantity());
            guardarLotes(lotes);
            return toResult(movimiento);
        });
    }

    @Override
    public StockMovementResult release(ReleaseStockCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            SparePartId partId = new SparePartId(cmd.sparePartId());
            BranchId branchId = new BranchId(cmd.branchId());
            validateEntitiesExist(partId, branchId, tenantId);

            List<StockLot> lotes = lotRepository.bloquearLotes(tenantId, partId, branchId);

            StockMovement movimiento = StockMovement.create(tenantId, partId, branchId, MovementType.RELEASE,
                    Quantity.of(cmd.quantity()), cmd.workOrderId(), claveDe(cmd.idempotencyKey()));

            Optional<StockMovement> yaReclamado = movementRepository.claim(movimiento);
            if (yaReclamado.isPresent()) {
                return toResult(yaReclamado.get());
            }

            liberar(lotes, cmd.quantity());
            guardarLotes(lotes);
            return toResult(movimiento);
        });
    }

    /**
     * Confirma una reserva: el stock reservado se consume y la reserva deja de
     * estar activa.
     *
     * <p>Sin esta operacion, una reserva confirmada por la orden de trabajo
     * nunca se convertia en una salida real, y el stock quedaba reservado
     * para siempre.
     */
    @Override
    public StockMovementResult commit(ReserveStockCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            SparePartId partId = new SparePartId(cmd.sparePartId());
            BranchId branchId = new BranchId(cmd.branchId());
            validateEntitiesExist(partId, branchId, tenantId);

            List<StockLot> lotes = lotRepository.bloquearLotes(tenantId, partId, branchId);

            StockMovement movimiento = StockMovement.create(tenantId, partId, branchId, MovementType.COMMIT,
                    Quantity.of(cmd.quantity()), cmd.workOrderId(), claveDe(cmd.idempotencyKey()));

            Optional<StockMovement> yaReclamado = movementRepository.claim(movimiento);
            if (yaReclamado.isPresent()) {
                return toResult(yaReclamado.get());
            }

            consumirReservado(lotes, cmd.quantity());
            guardarLotes(lotes);
            return toResult(movimiento);
        });
    }

    /**
     * Saldo del repuesto en la sucursal, con su VALOR.
     *
     * <p>El valor sale de los lotes que quedan, no de la suma de los
     * movimientos: el saldo dice cuantas unidades hay, y el valor dice cuanto
     * valen.
     */
    @Override
    public StockBalanceResult getBalance(UUID sparePartId, UUID branchId, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> saldoDe(sparePartId, branchId, tenantId));
    }

    private StockBalanceResult saldoDe(UUID sparePartId, UUID branchId, UUID tenantId) {
        TenantId tId = new TenantId(tenantId);
        SparePartId pId = new SparePartId(sparePartId);
        BranchId bId = new BranchId(branchId);

        List<StockLot> lotes = lotRepository.listar(tId, pId, bId);

        int entradas = 0;
        int totalReservado = 0;
        int disponible = 0;
        BigDecimal valorDisponible = BigDecimal.ZERO;
        BigDecimal valorReservado = BigDecimal.ZERO;

        for (StockLot lote : lotes) {
            entradas += lote.getQuantity();
            totalReservado += lote.getReservedQuantity();
            disponible += lote.getAvailableQuantity();
            valorDisponible = valorDisponible.add(lote.getAvailableValue());
            valorReservado = valorReservado.add(lote.getReservedValue());
        }

        // Las salidas ya se descontaron de los lotes al consumirlos.
        int salidas = Math.max(0, entradas - disponible - totalReservado);

        return new StockBalanceResult(sparePartId, branchId, entradas, salidas, totalReservado,
                disponible, valorDisponible, valorReservado);
    }

    private StockMovement reclamar(StockMovement movimiento) {
        Optional<StockMovement> yaReclamado = movementRepository.claim(movimiento);
        return yaReclamado.orElse(movimiento);
    }

    private IdempotencyKey claveDe(String idempotencyKey) {
        return idempotencyKey != null && !idempotencyKey.isBlank()
                ? IdempotencyKey.of(idempotencyKey)
                : null;
    }

    private int disponibles(List<StockLot> lotes) {
        return lotes.stream().mapToInt(StockLot::getAvailableQuantity).sum();
    }

    /** Consume los lotes mas antiguos primero: FIFO. */
    private void consumir(List<StockLot> lotes, int unidades) {
        int restantes = unidades;
        for (StockLot lote : lotes) {
            if (restantes <= 0) {
                break;
            }
            int delLote = Math.min(restantes, lote.getAvailableQuantity());
            if (delLote <= 0) {
                continue;
            }
            lote.consume(delLote);
            restantes = restantes - delLote;
        }
    }

    /** Consume unidades YA RESERVADAS de los lotes mas antiguos primero. */
    private void consumirReservado(List<StockLot> lotes, int unidades) {
        int restantes = unidades;
        for (StockLot lote : lotes) {
            if (restantes <= 0) {
                break;
            }
            int delLote = Math.min(restantes, lote.getReservedQuantity());
            if (delLote <= 0) {
                continue;
            }
            lote.consumeReserved(delLote);
            restantes = restantes - delLote;
        }
    }

    /** Reserva unidades de los lotes mas antiguos primero. */
    private void reservar(List<StockLot> lotes, int unidades) {
        int restantes = unidades;
        for (StockLot lote : lotes) {
            if (restantes <= 0) {
                break;
            }
            int delLote = Math.min(restantes, lote.getAvailableQuantity());
            if (delLote <= 0) {
                continue;
            }
            lote.reserve(delLote);
            restantes = restantes - delLote;
        }
    }

    /** Devuelve unidades reservadas de los lotes mas antiguos primero. */
    private void liberar(List<StockLot> lotes, int unidades) {
        int restantes = unidades;
        for (StockLot lote : lotes) {
            if (restantes <= 0) {
                break;
            }
            int delLote = Math.min(restantes, lote.getReservedQuantity());
            if (delLote <= 0) {
                continue;
            }
            lote.release(delLote);
            restantes = restantes - delLote;
        }
    }

    private void guardarLotes(List<StockLot> lotes) {
        lotes.forEach(lotRepository::guardar);
    }

    private void validateEntitiesExist(SparePartId partId, BranchId branchId, TenantId tenantId) {
        if (!sparePartRepository.existsById(partId, tenantId)) {
            throw new SparePartNotFoundException(partId.getValue().toString());
        }
        if (!branchRepository.existsById(branchId, tenantId)) {
            throw new BranchNotFoundException(branchId.getValue().toString());
        }
    }

    private StockMovementResult toResult(StockMovement m) {
        return new StockMovementResult(
                m.getId().getValue(), m.getTenantId().getValue(),
                m.getSparePartId().getValue(), m.getBranchId().getValue(),
                m.getMovementType().name(), m.getQuantity().getValue(),
                m.getReferenceId(), m.getIdempotencyKey() == null ? null : m.getIdempotencyKey().getValue(),
                m.getCreatedAt()
        );
    }
}
