CREATE TABLE IF NOT EXISTS branches (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- La Row-Level Security se HABILITA y se FUERZA. FORCE por si solo no la
-- habilita: solo obliga a que la politica se aplique tambien al propietario de
-- la tabla, que es el rol que la eludia. Habilitarla y forzarla son dos
-- sentencias distintas, y hacen falta las dos.
ALTER TABLE branches ENABLE ROW LEVEL SECURITY;
ALTER TABLE branches FORCE ROW LEVEL SECURITY;

-- El contexto de taller se lee SIN el flag "missing_ok": si no esta
-- establecido, la consulta falla de forma explicita en lugar de devolver
-- cero filas en silencio. El aislamiento no se relaja calladamente.
DROP POLICY IF EXISTS branch_tenant_isolation ON branches;
CREATE POLICY branch_tenant_isolation ON branches
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE TABLE IF NOT EXISTS spare_parts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    sku         VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    min_stock   INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_spare_part_tenant_sku UNIQUE (tenant_id, sku)
);

ALTER TABLE spare_parts ENABLE ROW LEVEL SECURITY;
ALTER TABLE spare_parts FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS spare_part_tenant_isolation ON spare_parts;
CREATE POLICY spare_part_tenant_isolation ON spare_parts
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE TABLE IF NOT EXISTS stock_movements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    spare_part_id   UUID NOT NULL,
    branch_id       UUID NOT NULL,
    movement_type   VARCHAR(20) NOT NULL,
    quantity        INTEGER NOT NULL,
    reference_id    UUID,
    idempotency_key VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_movement_part FOREIGN KEY (spare_part_id) REFERENCES spare_parts(id),
    CONSTRAINT fk_movement_branch FOREIGN KEY (branch_id) REFERENCES branches(id),
    CONSTRAINT uq_movement_idempotency UNIQUE (tenant_id, idempotency_key)
);

ALTER TABLE stock_movements ENABLE ROW LEVEL SECURITY;
ALTER TABLE stock_movements FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS stock_movement_tenant_isolation ON stock_movements;
CREATE POLICY stock_movement_tenant_isolation ON stock_movements
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);
