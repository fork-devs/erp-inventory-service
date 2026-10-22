# Documentación de Arquitectura e Integración — `erp-inventory-service`

**Microservicio:** Repuestos y Stock (Inventory)  
**Sistema:** DriveOS ERP SaaS  
**Organización:** ForkDevs — Universidad Peruana de Ciencias Aplicadas (UPC)  
**Curso:** Fundamentos de Arquitectura de Software (1ASI0657)

---

## 1. Visión General del Microservicio

El microservicio `erp-inventory-service` es el responsable del **catálogo de
repuestos** y del **stock por sucursal** de los talleres mecánicos que operan
sobre la plataforma DriveOS ERP SaaS.

Es el **origen de la verdad del inventario**: las órdenes de trabajo consumen
sus existencias, y el valor de lo que el taller tiene en el estante es lo que
permite decidir qué reponer.

```
                    ┌─────────────────────────┐
                    │   DriveOS Frontend      │
                    │  (Angular / Mobile)     │
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
         │               erp-inventory-service               │  Puerto 8084
         │        (Clean Architecture / Spring Boot)         │
         └───────┬─────────────────────────────┬───────────┘
                 │                             │
        JDBC/SQL │ (Postgres RLS)             │ gRPC/REST
                 ▼                             ▼
    ┌───────────────────────┐     ┌────────────────────────┐
    │     erp_inventory_db   │     │  erp-work-order-service│
    │     (Puerto 5436)     │     │  (saga de la reserva)  │
    └───────────────────────┘     └────────────────────────┘
```

---

## 2. Arquitectura de Software (Clean Architecture)

El servicio sigue estrictamente los principios de **Clean Architecture**
(Arquitectura Limpia) y **Domain-Driven Design (DDD)**, estructurado en cuatro
capas concéntricas donde las dependencias siempre apuntan hacia adentro:

```
      interfaces.rest (Controllers, DTOs, Exception Handlers)
                 │
                 ▼
      application (Use Cases, Services, Commands, Results)
                 │
                 ▼
      domain (Entities, Value Objects, Ports, Domain Exceptions)
                 ▲
                 │
      infrastructure (JPA Entities, Adapters, Config, Repositories)
```

### Estructura de Paquetes (`com.erp.inventory`):

```
com.erp.inventory/
├── domain/                                  # CAPA CORE: Reglas de negocio puras (sin Spring)
│   ├── exception/                           # Excepciones de negocio (con código estable)
│   ├── event/                               # Eventos de dominio (StockReserved, StockReleased, StockLow)
│   ├── model/                               # Entidades y agregados (Branch, SparePart, StockMovement, StockLot)
│   ├── port/                                # Puertos de salida hacia infraestructura
│   └── valueobject/                         # Inmutables con validación (TenantId, Sku, Quantity, StockLotId)
│
├── application/                             # CAPA DE APLICACIÓN: Orquestación de casos de uso
│   ├── dto/
│   │   ├── command/                         # Comandos inmutables (ReserveStockCommand, ...)
│   │   └── result/                          # Resultados inmutables hacia las capas externas
│   ├── logging/                             # Registro estructurado de los eventos de seguridad
│   ├── service/                             # Implementaciones de los casos de uso
│   └── usecase/                             # Interfaces de casos de uso (puertos de entrada)
│
├── infrastructure/                          # CAPA DE INFRAESTRUCTURA: Detalles tecnológicos
│   ├── config/                              # Seguridad stateless, CORS, OpenAPI, cableado Spring
│   ├── messaging/                           # Bandeja de salida y publicador de eventos
│   ├── persistence/                         # Entidades JPA, adaptadores, mappers, repositorios
│   ├── health/                              # Comprobaciones de salud y disponibilidad
│   └── security/                            # Verificación del token y contexto de taller
│
└── interfaces/rest/                         # CAPA DE ENTRADA: Controladores HTTP
    ├── advice/                              # Manejo global de excepciones (ProblemDetail)
    ├── controller/                          # BranchController, SparePartController, StockController
    ├── dto/
    │   ├── request/                         # Cuerpos de petición con validaciones (@Valid)
    │   └── response/                        # Respuestas HTTP presentadas al cliente
    └── support/                             # Contexto de taller de la petición
```

---

## 3. Modelo de Base de Datos y Aislamiento Multi-Tenant

### 3.1. Base de Datos Dedicada
Cumpliendo el principio de **Database-per-Service**, `erp-inventory-service` posee su propia base de datos física/lógica:
* **Motor:** PostgreSQL 16
* **Nombre:** `erp_inventory_db`
* **Puerto:** 5436
* **Herramienta de migración:** Flyway (`V1__create_customers_table.sql` … `V4__stock_outbox.sql`)

### 3.2. Aislamiento con Row Level Security (RLS)
Para garantizar la estricta separación de datos entre diferentes talleres (inquilinos):
1. Todas las tablas de negocio incluyen una columna obligatoria `tenant_id UUID NOT NULL`.
2. La RLS se **habilita y se fuerza** sobre las cuatro tablas. `FORCE` por sí solo no la habilita: solo obliga a que la política se aplique también al propietario, que es el rol que la eludía.
   ```sql
   ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
   ALTER TABLE customers FORCE  ROW LEVEL SECURITY;

   CREATE POLICY customer_tenant_isolation ON customers
       FOR ALL
       USING      (tenant_id = current_setting('app.tenant_id')::uuid)
       WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);
   ```
3. El contexto de taller se lee **sin** el flag `missing_ok`: sin contexto, la consulta **falla** de forma explícita en lugar de devolver cero filas en silencio.
4. La aplicación conecta con `erp_inventory_app`, que **no** es propietario de las tablas y **no** tiene `BYPASSRLS`. El propietario (`erp_inventory_owner`) lo usan solo las migraciones. Un rol de plataforma (`erp_fleet_platform`) recorre la bandeja de salida.
5. El contexto lo establece el **principal autenticado**, nunca la fila que se opera, y se aplica con `SET LOCAL` **dentro** de la transacción, con el orden de advice declarado.

### 3.3. Costeo FIFO por lotes
El saldo **no** es la suma de los movimientos: eso dice cuántas unidades hay, pero no cuánto valen. Cada entrada abre un **lote** (`stock_lots`) con su coste unitario real y su fecha, y las salidas y las reservas **consumen primero el lote más antiguo** (FIFO). La reserva se marca **sobre el lote** (`reserved_quantity`), de modo que el valor del stock reservado sale de la misma fila que el disponible. El coste de referencia del repuesto (`spare_parts.unit_cost`) es el precio esperado y sirve para detectar compras desviadas.

Decisión: `docs/adr/ADR-004-costeo-fifo-por-lotes.md` (copia canónica en este repositorio).

### 3.4. Relación Lógica entre Entidades
No existen claves foráneas cruzadas entre microservicios:
* La tabla `stock_movements` almacena la clave `spare_part_id` y `branch_id`, que son **referencias internas** de este mismo servicio.

---

## 4. Integraciones y Patrón Saga (Reserva Idempotente)

### 4.1. Conexión Entrada: `erp-api-gateway` → `erp-inventory-service`
* **Protocolo:** HTTP / REST síncrono, bajo el prefijo `/api/v1/inventory`, que es el `path-prefix` que el gateway reenvía **sin quitarlo**.
* **Seguridad:** el gateway valida el JWT emitido por `erp-auth-service` y reenvía las cabeceras. El servicio **vuelve a validar el token** contra el JWKS del emisor y deriva el taller del claim `tenant_id` del token verificado: la cabecera `X-Tenant-ID` es una **pista** que se contrasta con el token, nunca una autoridad.

### 4.2. Participación en la Saga de Órdenes de Trabajo
* **Reserva:** `POST /api/v1/inventory/stock/reserve` con `idempotencyKey` (ej. `saga-wo-123-reserve-part1`).
* **Confirmación:** `POST /api/v1/inventory/stock/commit` con su propia `idempotencyKey`. Sin ella, la reserva aprobada nunca se convertía en una salida real y el stock quedaba reservado para siempre.
* **Liberación (rollback):** `POST /api/v1/inventory/stock/release` para liberar las unidades reservadas.
* **Idempotencia:** la clave se **reclama en la base de datos** con la propia inserción del movimiento (`ON CONFLICT DO NOTHING`): dos peticiones concurrentes con la misma clave no pueden registrar dos movimientos, y la que pierde la carrera devuelve el resultado de la que la ganó.
* **Concurrencia:** el disponible se calcula con los **lotes bloqueados** (`FOR UPDATE`), en orden de llegada: dos reservas simultáneas se serializan y no pueden sobre-reservar.

---

## 5. Matriz de Endpoints REST

Documentación interactiva disponible (perfil de desarrollo):  
👉 **`http://localhost:8084/swagger-ui/index.html`**

Todos los endpoints exigen un **token de acceso verificado** y declaran el rol que exigen. El taller se deriva del token.

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| **POST** | `/api/v1/inventory/branches` | Registrar una sucursal o almacén | `SUPERADMIN`, `ADMIN_TALLER`, `ASESOR` |
| **GET** | `/api/v1/inventory/branches` | Listar sucursales del taller | + `MECANICO` |
| **GET** | `/api/v1/inventory/branches/{id}` | Obtener sucursal por ID | + `MECANICO` |
| **POST** | `/api/v1/inventory/spare-parts` | Crear repuesto en el catálogo | `SUPERADMIN`, `ADMIN_TALLER`, `ASESOR` |
| **GET** | `/api/v1/inventory/spare-parts` | Listar repuestos del catálogo | + `MECANICO` |
| **GET** | `/api/v1/inventory/spare-parts/{id}` | Obtener repuesto por ID | + `MECANICO` |
| **POST** | `/api/v1/inventory/stock/movements` | Registrar entrada (`IN`) o salida (`OUT`) manual, con su coste | `SUPERADMIN`, `ADMIN_TALLER`, `ASESOR` |
| **POST** | `/api/v1/inventory/stock/reserve` | Reservar repuestos para una orden (saga idempotente) | `SUPERADMIN`, `ADMIN_TALLER`, `ASESOR` |
| **POST** | `/api/v1/inventory/stock/commit` | Confirmar la reserva: consume el stock reservado | `SUPERADMIN`, `ADMIN_TALLER`, `ASESOR` |
| **POST** | `/api/v1/inventory/stock/release` | Liberar reserva ante cancelación de orden | `SUPERADMIN`, `ADMIN_TALLER`, `ASESOR` |
| **GET** | `/api/v1/inventory/stock/balance/{sparePartId}/branch/{branchId}` | Consultar saldo, stock reservado y disponible, **con su valor** | + `MECANICO` |

**Errores.** El cuerpo del error es un `ProblemDetail` (RFC 9457) con un código estable por familia (`code`) y el taller (`tenant_id`): `INVENTORY_SKU_DUPLICADO`, `INVENTORY_INSUFFICIENT_STOCK`, `INVENTORY_BRANCH_NOT_FOUND`, `INVENTORY_PART_NOT_FOUND`, `INVENTORY_ACCESS_DENIED`, `INVENTORY_UNAUTHENTICATED`, `INVENTORY_VALIDATION_ERROR`, `INVENTORY_NOT_FOUND`, `INVENTORY_INTERNAL_ERROR`.

---

## 6. Eventos de Stock

Los cambios de stock relevantes (`StockReserved`, `StockReleased`, `StockLow`) se persisten en una **bandeja de salida**, en la **misma transacción** que confirma el cambio, y los publica un relay con reintentos de espera creciente y cola de muertos. El transporte sigue siendo el `EventPublisherPort`: **Kafka todavía no existe** en la plataforma, y la bandeja no depende del bus.

El identificador del evento viaja en el mensaje y no se regenera en cada reintento, de modo que un consumidor pueda **deduplicar**: la entrega es al menos una vez, y el identificador es lo que hace posible la deduplicación.

---

## 7. Guía de Ejecución Local

```bash
# 1. Posicionarse en el directorio
cd erp-inventory-service

# 2. Definir la contraseña del rol propietario
export SPRING_FLYWAY_PASSWORD=una-contrasena-local

# 3. Levantar la base de datos PostgreSQL en Docker
docker compose up -d

# 4. Compilar y ejecutar la aplicación mediante el Maven Wrapper
./mvnw spring-boot:run
```

El servicio estará listo para recibir peticiones en el puerto **`8084`**, bajo
el prefijo **`/api/v1/inventory`**. Sin un token de acceso verificado, la
API responde `401`.
