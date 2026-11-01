package com.erp.inventory.domain;

import com.erp.inventory.domain.exception.BranchNotFoundException;
import com.erp.inventory.domain.exception.DomainException;
import com.erp.inventory.domain.exception.InsufficientStockException;
import com.erp.inventory.domain.exception.SkuAlreadyExistsException;
import com.erp.inventory.domain.exception.SparePartNotFoundException;
import com.erp.inventory.domain.exception.TenantContextMissingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying that domain exceptions maintain stable error codes.
 *
 * <p>Error codes constitute the client API contract: exception messages may vary,
 * but error codes must remain stable across versions.
 */
class DomainExceptionTest {

    @Test
    @DisplayName("Cada familia de error tiene su codigo")
    void cadaFamiliaDeErrorTieneSuCodigo() {
        assertEquals("INVENTORY_SKU_DUPLICADO", new SkuAlreadyExistsException("ABC-1").getCode());
        assertEquals("INVENTORY_INSUFFICIENT_STOCK", new InsufficientStockException("no hay").getCode());
        assertEquals("INVENTORY_BRANCH_NOT_FOUND", new BranchNotFoundException("b-1").getCode());
        assertEquals("INVENTORY_PART_NOT_FOUND", new SparePartNotFoundException("p-1").getCode());
        assertEquals("INVENTORY_TENANT_CONTEXT_MISSING", new TenantContextMissingException().getCode());
    }

    @Test
    @DisplayName("El error de dominio sin codigo propio tiene el codigo por defecto")
    void elErrorDeDominioSinCodigoPropioTieneElCodigoPorDefecto() {
        assertEquals("INVENTORY_ERROR", new DomainException("algo").getCode());
    }

    @Test
    @DisplayName("El mensaje explica lo ocurrido")
    void elMensajeExplicaLoOcurrido() {
        assertTrue(new SkuAlreadyExistsException("ABC-1").getMessage().contains("ABC-1"));
        assertTrue(new BranchNotFoundException("b-1").getMessage().contains("b-1"));
        assertTrue(new SparePartNotFoundException("p-1").getMessage().contains("p-1"));
    }
}
