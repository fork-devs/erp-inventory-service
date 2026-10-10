# Documentación de Arquitectura e Integración — `erp-inventory-service`

**Microservicio:** Inventario, Catálogo de Repuestos y Stock (Inventory & Spare Parts)  
**Sistema:** DriveOS ERP SaaS  
**Organización:** ForkDevs — Universidad Peruana de Ciencias Aplicadas (UPC)  
**Curso:** Fundamentos de Arquitectura de Software (1ASI0657)  

---

## 1. Visión General del Microservicio

El microservicio `erp-inventory-service` es el componente responsable de la gestión de repuestos, catálogo de partes automotrices, sucursales/almacenes y operaciones de movimiento de inventario para los talleres en DriveOS ERP.

Desempeña un rol crítico en la **Saga Transaccional** coordinada con `erp-work-order-service`, proveyendo operaciones de **Reserva y Liberación de Stock con idempotencia** para garantizar que los mecánicos cuenten con los suministros necesarios sin generar sobreventa de stock ni bloqueos distribuidos.

```
                    ┌─────────────────────────┐
                    │   DriveOS Frontend      │
                    │   (Angular / Mobile)    │
                    └───────────┬─────────────┘
                                │ HTTP / REST (JWT Bearer)
                                ▼
                    ┌─────────────────────────┐
                    │    erp-api-gateway      │  Puerto 8080
                    │  (Edge & Token Verify)  │
                    └───────────┬─────────────┘
                                │ HTTP / REST (Inyecta X-Tenant-ID, X-User-ID)
                                ▼
        ┌─────────────────────────────────────────────────┐
        │             erp-inventory-service               │  Puerto 8084
        │        (Clean Architecture / Spring Boot)       │
        └───────┬─────────────────────────────▲───────────┘
                │                             │
       JDBC/SQL │ (Postgres RLS)      HTTP/gRPC│ Reserva/Liberación (Saga)
                ▼                             │
   ┌───────────────────────┐     ┌────────────┴───────────┐
   │   erp_inventory_db    │     │ erp-work-order-service │  Puerto 8086
   │     (Puerto 5436)     │     │  (Órdenes de Trabajo)  │
   └───────────────────────┘     └────────────────────────┘
```

---

## 2. Arquitectura de Software (Clean Architecture)

El servicio sigue estrictamente los principios de **Clean Architecture** (Arquitectura Limpia) y **Domain-Driven Design (DDD)**, estructurado en cuatro capas concéntricas:

```
com.erp.inventory/
├── domain/                                  # REGLAS PURAS DE DOMINIO
│   ├── exception/                           # SparePartNotFound, BranchNotFound, InsufficientStock, SkuAlreadyExists
│   ├── model/                               # SparePart, Branch, StockMovement, MovementType
│   ├── port/                                # SparePartRepositoryPort, BranchRepositoryPort, StockMovementRepositoryPort
│   └── valueobject/                         # TenantId, SparePartId, BranchId, StockMovementId, Sku, Quantity, IdempotencyKey
│
├── application/                             # CASOS DE USO Y ORQUESTACIÓN
│   ├── dto/command/                         # RegisterSparePartCommand, RegisterBranchCommand, ReserveStockCommand, ReleaseStockCommand...
│   ├── dto/result/                          # SparePartResult, BranchResult, StockMovementResult, StockBalanceResult
│   ├── service/                             # BranchService, SparePartService, StockOperationService
│   └── usecase/                             # BranchUseCase, SparePartUseCase, StockOperationUseCase
│
├── infrastructure/                          # TECNOLOGÍAS Y PERSISTENCIA
│   ├── config/                              # SecurityConfig (Stateless), OpenApiConfig (@Hidden), ApplicationServiceConfig
│   └── persistence/
│       ├── adapter/                         # BranchPersistenceAdapter, SparePartPersistenceAdapter, StockMovementPersistenceAdapter
│       ├── entity/                          # BranchJpaEntity, SparePartJpaEntity, StockMovementJpaEntity
│       ├── mapper/                          # Mappers bidireccionales Dominio <-> Entidad JPA
│       └── repository/                      # Repositorios Spring Data JPA
│
└── interfaces/rest/                         # APIS Y PRESENTACIÓN
    ├── advice/                              # GlobalRestExceptionHandler (anotado con @Hidden)
    ├── controller/                          # BranchController, SparePartController, StockController
    └── dto/
        ├── request/                         # Validaciones Jakarta (@Valid, @NotNull, @Min)
        └── response/                        # DTOs de salida inmutables
```

---

## 3. Modelo de Base de Datos y Aislamiento Multi-Tenant

### 3.1. Base de Datos Dedicada
* **Motor:** PostgreSQL 16
* **Nombre de BD:** `erp_inventory_db`
* **Puerto:** 5436
* **Herramienta de migración:** Flyway (`V1__init_inventory_schema.sql`)

### 3.2. Aislamiento por Row Level Security (RLS)
Todas las tablas implementan políticas RLS que aseguran que ningún taller acceda al inventario o almacenes de otro:
```sql
ALTER TABLE branches ENABLE ROW LEVEL SECURITY;
CREATE POLICY branch_tenant_isolation ON branches
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::uuid);

ALTER TABLE spare_parts ENABLE ROW LEVEL SECURITY;
CREATE POLICY spare_part_tenant_isolation ON spare_parts
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::uuid);

ALTER TABLE stock_movements ENABLE ROW LEVEL SECURITY;
CREATE POLICY stock_movement_tenant_isolation ON stock_movements
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::uuid);
```

---

## 4. Integraciones y Patrón Saga (Reserva Idempotente)

### 4.1. Conexión Entrada: `erp-api-gateway` → `erp-inventory-service`
* Valida el JWT y propaga el header obligatorio `X-Tenant-ID`.

### 4.2. Participación en la Saga de Órdenes de Trabajo (`ReserveStock` / `ReleaseStock`)
* **Flujo Positivo:** Cuando se aprueba una orden de trabajo en `erp-work-order-service`, este solicita la reserva mediante `POST /api/v1/stock/reserve` enviando un `idempotencyKey` único (ej. `saga-wo-123-reserve-part1`).
* **Idempotencia:** Si la red falla y se reintenta la llamada, el servicio retorna la reserva previamente registrada sin duplicar el descuento de inventario.
* **Flujo Compensatorio (Rollback):** Si la orden de trabajo es cancelada o rechazada por el cliente, se emite una solicitud `POST /api/v1/stock/release` para liberar los artículos reservados.

---

## 5. Matriz de Endpoints REST

Documentación interactiva disponible en:  
👉 **`http://localhost:8084/swagger-ui/index.html`**

| Método | Endpoint | Descripción | Header Obligatorio |
|---|---|---|---|
| **POST** | `/api/v1/branches` | Registrar una nueva sucursal o almacén | `X-Tenant-ID` |
| **GET** | `/api/v1/branches` | Listar sucursales del taller | `X-Tenant-ID` |
| **GET** | `/api/v1/branches/{id}` | Obtener sucursal por ID | `X-Tenant-ID` |
| **POST** | `/api/v1/spare-parts` | Crear nuevo repuesto en el catálogo | `X-Tenant-ID` |
| **GET** | `/api/v1/spare-parts` | Listar repuestos del catálogo | `X-Tenant-ID` |
| **GET** | `/api/v1/spare-parts/{id}` | Obtener repuesto por ID | `X-Tenant-ID` |
| **POST** | `/api/v1/stock/movements` | Registrar entrada manual (`IN`) o salida (`OUT`) | `X-Tenant-ID` |
| **POST** | `/api/v1/stock/reserve` | Reservar repuestos para una orden (Saga Idempotente) | `X-Tenant-ID` |
| **POST** | `/api/v1/stock/release` | Liberar reserva ante cancelación de orden | `X-Tenant-ID` |
| **GET** | `/api/v1/stock/balance/{partId}/branch/{branchId}` | Consultar saldo, stock reservado y disponible | `X-Tenant-ID` |

---

## 6. Guía de Ejecución Local

```bash
# 1. Ubicarse en el directorio
cd "C:\Users\ASUS\Documents\FUNDAMENTOS\informefinal\Nueva carpeta\erp-inventory-service"

# 2. Levantar la base de datos PostgreSQL en Docker
docker compose up -d

# 3. Iniciar la aplicación
.\mvnw.cmd spring-boot:run
```
