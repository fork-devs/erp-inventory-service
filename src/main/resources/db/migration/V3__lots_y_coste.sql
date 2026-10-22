-- =============================================================================
-- ERP INVENTORY SERVICE - V3: LOTES Y COSTE
-- =============================================================================
-- El saldo era la suma de los movimientos: decia cuantas unidades habia, pero
-- no cuanto valian. El coste de lo consumido se perdia en el momento en que se
-- consumia, y el coste medio mezclaba las compras antiguas con las recientes,
-- de modo que el valor que salia no correspondia a ningun lote real.
--
-- Cada entrada abre un LOTE con su coste unitario real y su fecha. Las salidas
-- y las reservas consumen primero el lote mas antiguo (FIFO). El saldo
-- informa del valor de los lotes que quedan.
--
-- Hay dos costes con papeles distintos: el de REFERENCIA del repuesto, que
-- es el precio esperado, y el REAL de cada entrada, que puede diferir. El de
-- referencia sirve para avisar cuando una compra se aparta del precio
-- esperado; el real es el que valora el stock.
--
-- Ver docs/adr/ADR-004-costeo-fifo-por-lotes.md.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. El coste de referencia del repuesto
-- -----------------------------------------------------------------------------

ALTER TABLE spare_parts ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(14, 4) NOT NULL DEFAULT 0;

-- -----------------------------------------------------------------------------
-- 2. El coste real de cada movimiento
-- -----------------------------------------------------------------------------

ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(14, 4);

-- -----------------------------------------------------------------------------
-- 4. Los lotes
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS stock_lots (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    spare_part_id     UUID NOT NULL,
    branch_id         UUID NOT NULL,
    quantity          INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    unit_cost         NUMERIC(14, 4) NOT NULL,
    entry_date        TIMESTAMP NOT NULL DEFAULT now(),
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_lot_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_lot_reserved_within_quantity CHECK (reserved_quantity >= 0 AND reserved_quantity <= quantity),
    CONSTRAINT fk_lot_part FOREIGN KEY (spare_part_id) REFERENCES spare_parts(id),
    CONSTRAINT fk_lot_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
);

ALTER TABLE stock_lots ENABLE ROW LEVEL SECURITY;
ALTER TABLE stock_lots FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS stock_lot_tenant_isolation ON stock_lots;
CREATE POLICY stock_lot_tenant_isolation ON stock_lots
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

-- El consumo FIFO: los lotes de un repuesto y una sucursal, del mas antiguo al
-- mas reciente. Es el indice que recorre el consumo y el que serializa dos
-- reservas concurrentes del mismo repuesto.
CREATE INDEX IF NOT EXISTS idx_stock_lots_fifo
    ON stock_lots(tenant_id, spare_part_id, branch_id, entry_date);

-- Un lote por entrada: la entrada es la que abre el lote.
CREATE INDEX IF NOT EXISTS idx_stock_lots_movement
    ON stock_lots(tenant_id, spare_part_id, branch_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE stock_lots TO erp_inventory_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE stock_lots TO erp_inventory_platform;

-- -----------------------------------------------------------------------------
-- 3. El conjunto de tipos de movimiento
-- -----------------------------------------------------------------------------
-- El tipo de movimiento es un conjunto explicito: el que hay, el que sale, la
-- reserva, la liberacion y la CONSUMICION de la reserva. Un movimiento con un
-- tipo fuera del conjunto no se puede distinguir de uno que descuenta stock, y
-- el saldo miente.

ALTER TABLE stock_movements DROP CONSTRAINT IF EXISTS ck_movement_type;
ALTER TABLE stock_movements ADD CONSTRAINT ck_movement_type
    CHECK (movement_type IN ('IN', 'OUT', 'RESERVE', 'RELEASE', 'COMMIT'));

-- Ninguna tabla de negocio puede quedar sin taller: una fila sin tenant_id
-- no la cubre ninguna politica de aislamiento.
DO $$
DECLARE
    sin_taller TEXT;
BEGIN
    SELECT string_agg(nombre, ', ') INTO sin_taller
      FROM unnest(ARRAY['branches', 'spare_parts', 'stock_movements', 'stock_lots']) AS nombre
     WHERE NOT EXISTS (
        SELECT 1 FROM information_schema.columns c
          JOIN information_schema.tables t ON t.table_name = c.table_name
         WHERE c.table_schema = 'public' AND t.table_name = nombre
           AND c.column_name = 'tenant_id' AND c.is_nullable = 'NO'
     );

    IF sin_taller IS NOT NULL THEN
        RAISE EXCEPTION 'Estas tablas no tienen tenant_id obligatorio: %', sin_taller;
    END IF;

    IF EXISTS (
        SELECT 1 FROM pg_class c
          JOIN pg_namespace n ON n.oid = c.relnamespace
         WHERE n.nspname = 'public'
           AND c.relname IN ('branches', 'spare_parts', 'stock_movements', 'stock_lots')
           AND (NOT c.relrowsecurity OR NOT c.relforcerowsecurity)
    ) THEN
        RAISE EXCEPTION 'Hay tablas de negocio sin la Row-Level Security habilitada y forzada';
    END IF;
END
$$;

