-- =============================================================================
-- ERP INVENTORY SERVICE - V5: PRODUCTS AND PRODUCT BATCHES
-- =============================================================================
-- Extends the inventory capability with branch-scoped products and batch-level
-- stock tracking according to docs/references-api/inventory/products.md.
--
-- Enforces mandatory tenant_id, foreign keys, indexes, and forced PostgreSQL RLS.
-- =============================================================================

CREATE TABLE IF NOT EXISTS products (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL,
    branch_id     UUID NOT NULL,
    category      VARCHAR(100) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    sku           VARCHAR(100) NOT NULL,
    description   TEXT,
    sale_price    NUMERIC(14, 4) NOT NULL,
    minimum_stock INTEGER NOT NULL DEFAULT 0,
    is_active     BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_tenant_branch_sku UNIQUE (tenant_id, branch_id, sku),
    CONSTRAINT ck_product_sale_price_positive CHECK (sale_price >= 0),
    CONSTRAINT ck_product_minimum_stock_non_negative CHECK (minimum_stock >= 0),
    CONSTRAINT fk_product_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
);

ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE products FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS product_tenant_isolation ON products;
CREATE POLICY product_tenant_isolation ON products
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE INDEX IF NOT EXISTS idx_products_tenant_branch
    ON products(tenant_id, branch_id);

CREATE INDEX IF NOT EXISTS idx_products_tenant_category
    ON products(tenant_id, category);

CREATE TABLE IF NOT EXISTS product_batches (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          UUID NOT NULL,
    product_id         UUID NOT NULL,
    batch_number       VARCHAR(100) NOT NULL,
    quantity           INTEGER NOT NULL,
    remaining_quantity INTEGER NOT NULL,
    purchase_price     NUMERIC(14, 4) NOT NULL,
    expiration_date    TIMESTAMP,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_batch_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_batch_remaining_quantity_valid CHECK (remaining_quantity >= 0 AND remaining_quantity <= quantity),
    CONSTRAINT ck_batch_purchase_price_non_negative CHECK (purchase_price >= 0),
    CONSTRAINT uq_batch_tenant_product_number UNIQUE (tenant_id, product_id, batch_number),
    CONSTRAINT fk_batch_product FOREIGN KEY (product_id) REFERENCES products(id)
);

ALTER TABLE product_batches ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_batches FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS product_batch_tenant_isolation ON product_batches;
CREATE POLICY product_batch_tenant_isolation ON product_batches
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE INDEX IF NOT EXISTS idx_product_batches_tenant_product
    ON product_batches(tenant_id, product_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE products TO erp_inventory_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE products TO erp_inventory_platform;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE product_batches TO erp_inventory_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE product_batches TO erp_inventory_platform;
