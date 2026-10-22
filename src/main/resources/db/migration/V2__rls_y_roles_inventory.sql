-- =============================================================================
-- ERP INVENTORY SERVICE - V2: AISLAMIENTO EFECTIVO Y ROLES
-- =============================================================================
-- La aplicacion conectaba con el rol PROPIETARIO de las tablas. En PostgreSQL
-- el propietario elude su propia politica de Row-Level Security, de modo que la
-- politica declarada en V1 era inerte: con ella activa, el aislamiento no
-- aislaba nada.
--
-- Se separa el rol de ejecucion del rol de migracion, y se FUERZA la politica
-- para que se aplique tambien al propietario. El contexto de taller se lee sin
-- el flag "missing_ok": sin contexto, la consulta falla de forma explicita en
-- lugar de devolver cero filas en silencio.
--
-- El rol de PLATAFORMA existe para el relay de la bandeja de salida, que
-- recorre los mensajes pendientes de TODOS los talleres: es un proceso interno
-- y todavia no conoce el taller de cada mensaje. Su credencial vive separada
-- de la de la aplicacion.
--
-- Ver docs/doc-estado-implementacion.md, hallazgos P1 6.6, P1 6.14 y P1 6.53.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. La politica, forzada y con el contexto obligatorio
-- -----------------------------------------------------------------------------

ALTER TABLE branches ENABLE ROW LEVEL SECURITY;
ALTER TABLE branches FORCE ROW LEVEL SECURITY;

ALTER TABLE spare_parts ENABLE ROW LEVEL SECURITY;
ALTER TABLE spare_parts FORCE ROW LEVEL SECURITY;

ALTER TABLE stock_movements ENABLE ROW LEVEL SECURITY;
ALTER TABLE stock_movements FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS branch_tenant_isolation ON branches;
CREATE POLICY branch_tenant_isolation ON branches
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

DROP POLICY IF EXISTS spare_part_tenant_isolation ON spare_parts;
CREATE POLICY spare_part_tenant_isolation ON spare_parts
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

DROP POLICY IF EXISTS stock_movement_tenant_isolation ON stock_movements;
CREATE POLICY stock_movement_tenant_isolation ON stock_movements
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

-- -----------------------------------------------------------------------------
-- 2. Rol de ejecucion y rol de plataforma
-- -----------------------------------------------------------------------------

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'erp_inventory_app') THEN
        EXECUTE format(
            'CREATE ROLE erp_inventory_app LOGIN NOINHERIT NOSUPERUSER NOCREATEDB '
            'NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD %L',
            '${appRolePassword}'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'erp_inventory_platform') THEN
        EXECUTE format(
            'CREATE ROLE erp_inventory_platform LOGIN NOINHERIT NOSUPERUSER NOCREATEDB '
            'NOCREATEROLE NOREPLICATION BYPASSRLS PASSWORD %L',
            '${platformRolePassword}'
        );
    END IF;
END
$$;

-- Sin USAGE sobre el esquema, el rol no resuelve ningun objeto aunque tenga
-- privilegios sobre las tablas.
GRANT USAGE ON SCHEMA public TO erp_inventory_app;
GRANT USAGE ON SCHEMA public TO erp_inventory_platform;

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE branches TO erp_inventory_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE spare_parts TO erp_inventory_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE stock_movements TO erp_inventory_app;

-- El relay de la bandeja recorre los mensajes pendientes de todos los
-- talleres: sus ESCRITURAS tienen que usar el mismo alcance que sus lecturas.
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE stock_movements TO erp_inventory_platform;

-- Comprobacion de que el rol de ejecucion no puede eludir la politica de
-- aislamiento: si pudiera, todo lo anterior seria inerte.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_roles
        WHERE rolname = 'erp_inventory_app' AND rolbypassrls
    ) THEN
        RAISE EXCEPTION 'El rol erp_inventory_app no debe tener BYPASSRLS: la politica de aislamiento seria eludible';
    END IF;
END
$$;
