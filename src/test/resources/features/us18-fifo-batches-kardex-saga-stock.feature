@US18 @EP07 @erp-inventory-service
# User Story: US18 - Register purchase batches and execute FIFO costing, kardex movements, and Saga stock reservation
Feature: FIFO Batch Costing, Kardex Movements, and Saga Stock Reservation
  As a warehouse manager or service advisor
  I want to record purchase batches, reserve stock for work orders, commit FIFO consumption, and release reservations
  So that work order margins are protected and stock overselling is prevented

  Background:
    Given the "erp-inventory-service" REST API is available at "/api/v1/inventory"

  @US18 @happy-path
  # User Story: US18 - Receive purchase batch and reserve then commit stock using FIFO
  Scenario: Purchase batch receipt enables Saga stock reservation and FIFO commit
    Given an authenticated user with role "SERVICE_ADVISOR" in tenant "tenant-lima-01"
    And a product "prod-uuid-01" in branch "branch-uuid-01"
    When the client sends a "POST" request to "/api/v1/inventory/products/prod-uuid-01/batches" with JSON payload:
      """
      {
        "branchId": "branch-uuid-01",
        "quantity": 20,
        "unitCost": 28.50,
        "receivedAt": "2026-11-10T09:00:00Z"
      }
      """
    Then the response status code should be 201
    When the client sends a "POST" request to "/api/v1/inventory/stock/reserve" with JSON payload:
      """
      {
        "workOrderId": "wo-uuid-01",
        "branchId": "branch-uuid-01",
        "productId": "prod-uuid-01",
        "quantity": 4
      }
      """
    Then the response status code should be 200
    And the JSON response field "reservedQuantity" should equal 4

  @US18 @negative
  # User Story: US18 - Reject stock reservation when available quantity is insufficient
  Scenario: Reserving more stock than available returns HTTP 409 Conflict with shortage details
    Given an authenticated user with role "SERVICE_ADVISOR"
    And product "prod-uuid-01" in branch "branch-uuid-01" has only 2 available units
    When the client sends a "POST" request to "/api/v1/inventory/stock/reserve" with JSON payload:
      """
      {
        "workOrderId": "wo-uuid-02",
        "branchId": "branch-uuid-01",
        "productId": "prod-uuid-01",
        "quantity": 10
      }
      """
    Then the response status code should be 409
    And the JSON response should contain an error message indicating insufficient stock
