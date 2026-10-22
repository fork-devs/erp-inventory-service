-- =============================================================================
-- ERP INVENTORY SERVICE - V4: BANDEJA DE SALIDA DE LOS EVENTOS DE STOCK
-- =============================================================================
-- Los cambios de stock relevantes se persisten junto a su evento, en la misma
-- transaccion local, y un relay los publica despues.
--
-- Publicar el evento DENTRO de la transaccion que confirma el cambio de
-- stock es justo lo que el patron evita: el bus podria recibir un evento que
-- la base de datos no llego a confirmar, y un fallo del bus perderia el
-- evento sin revertir el stock.
--
-- El transporte sigue siendo el EventPublisherPort: Kafka todavia no existe
-- en la plataforma. La bandeja no depende del bus.
--
-- Ver docs/doc-estado-implementacion.md, hallazgos P1 6.8 y P1 7.21.
-- =============================================================================

CREATE TABLE IF NOT EXISTS stock_outbox (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- El identificador del evento viaja en el mensaje y no se regenera en cada
    -- reintento, de modo que un consumidor pueda deduplicar.
    event_id        UUID NOT NULL UNIQUE,
    aggregate_type  VARCHAR(50) NOT NULL,
    aggregate_id    UUID NOT NULL,
    tenant_id       UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         TEXT NOT NULL,
    attempts        INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_error      TEXT,
    -- El estado es explicito y no un booleano: con un booleano no se puede
    -- distinguir un mensaje recien creado de uno que lleva horas fallando.
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- El relay recorre los pendientes por fecha de proximo intento.
CREATE INDEX IF NOT EXISTS idx_stock_outbox_pending
    ON stock_outbox(next_attempt_at)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_stock_outbox_dead_letter
    ON stock_outbox(tenant_id, created_at DESC)
    WHERE status = 'DEAD_LETTER';

-- La bandeja NO lleva aislamiento por taller, a diferencia de las tablas de
-- negocio. Es una cola INTERNA del servicio: el relay tiene que recorrer los
-- mensajes pendientes de TODOS los talleres, porque todavia no conoce el
-- taller de cada uno, y ningun endpoint expone la bandeja. Es el mismo
-- criterio que sigue el servicio de talleres con su propia bandeja.
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE stock_outbox TO erp_inventory_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE stock_outbox TO erp_inventory_platform;
