@US17 @EP07 @erp-inventory-service
# User Story: US17 - Configure branch warehouses and manage product and OEM spare parts catalog
Feature: Branch Warehouses, Product Master, and OEM Spare Parts Catalog
  As a workshop administrator or warehouse manager
  I want to create branch warehouses and manage products and OEM-compatible spare parts
  So that multi-branch stock balances and part compatibility remain accurate

  Background:
    Given the "erp-inventory-service" REST API is available at "/api/v1/inventory"

  @US17 @happy-path
  # User Story: US17 - Create a branch warehouse and register an OEM product
  Scenario: Warehouse manager creates a warehouse and registers a product with SKU and OEM reference
    Given an authenticated user with role "WORKSHOP_ADMIN" in tenant "tenant-lima-01"
    When the client sends a "POST" request to "/api/v1/inventory/branches" with JSON payload:
      """
      {
        "branchId": "branch-uuid-01",
        "name": "Almacen Central Surquillo",
        "code": "WH-SUR-01"
      }
      """
    Then the response status code should be 201
    When the client sends a "POST" request to "/api/v1/inventory/products" with JSON payload:
      """
      {
        "sku": "OIL-5W30-SYN",
        "name": "Synthetic Motor Oil 5W-30 1L",
        "category": "LUBRICANTS",
        "salePrice": 45.00,
        "minStockThreshold": 10
      }
      """
    Then the response status code should be 201
    And the JSON response should contain a generated "id"
    And the JSON response field "sku" should equal "OIL-5W30-SYN"

  @US17 @negative
  # User Story: US17 - Reject negative sale price or negative minimum stock threshold
  Scenario: Registering a product with a negative sale price returns HTTP 400 Bad Request
    Given an authenticated user with role "WORKSHOP_ADMIN"
    When the client sends a "POST" request to "/api/v1/inventory/products" with JSON payload:
      """
      {
        "sku": "BAD-PRICE-01",
        "name": "Invalid Price Part",
        "category": "FILTERS",
        "salePrice": -15.00,
        "minStockThreshold": 5
      }
      """
    Then the response status code should be 400
    And the JSON response should contain a validation error for "salePrice"
