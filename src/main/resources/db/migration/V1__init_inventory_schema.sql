CREATE TABLE IF NOT EXISTS branches (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE branches ENABLE ROW LEVEL SECURITY;

CREATE POLICY branch_tenant_isolation ON branches
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::uuid);

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

CREATE POLICY spare_part_tenant_isolation ON spare_parts
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::uuid);

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

CREATE POLICY stock_movement_tenant_isolation ON stock_movements
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::uuid);
