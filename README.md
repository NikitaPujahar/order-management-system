# order-management-system
Bygghemma - Assessment task

 ## Prerequisites

- **Java**: 11 or higher
- **Maven**: 3.6+

---

	## Building and Running

### Build the Project
```bash
mvn clean compile
```

### Run Tests
```bash
mvn test
```

### Run the Application
```bash
mvn exec:java -Dexec.mainClass="com.oms.OrderManagementApplication"
```

**Example:**
```bash
username@username:~/assignment/order-management-system$ mvn exec:java -Dexec.mainClass="com.oms.OrderManagementApplication"
```

	
  ## Design Decisions

### 1. Architecture
**Layered Architecture** with clear separation of concerns:
- **Model Layer**: Domain entities (Order, Customer, Product, Item)
- **Repository Layer**: In-memory data storage using `ConcurrentHashMap` for thread-safety
- **Service Layer**: Business logic (OrderManagementService, PricingEngine, InventoryManager)
- **Exception Layer**: Custom exceptions for different error scenarios

### 2. Concurrency Handling
**Synchronized Inventory Management**: The `InventoryManager` uses synchronized methods to handle concurrent access. When multiple orders compete for limited stock:
- Stock availability checks and reservations happen atomically
- Reservations are tracked separately from actual inventory
- Only confirmed orders reduce actual stock levels
- This ensures the "two customers, one product" scenario works correctly

### 3. Pricing Strategy
**VAT-Inclusive Pricing**: All prices include 25% Swedish VAT
- Base prices stored with VAT included
- Discounts applied to VAT-inclusive amounts
- VAT extracted from final price using formula: `VAT = total - (total / 1.25)`
- Calculation order: customer discount → bulk discount → VAT extraction

### 4. Order State Machine
**Explicit State Transitions**: Orders follow a strict lifecycle:
```
CREATED → PENDING_VALIDATION → VALIDATED → PAID → FULFILLED
                ↓
            CANCELLED (from any state except FULFILLED)
```

`InvalidStateTransitionException` prevents illegal transitions.

### 5. Validation Order
Validations execute in a specific sequence to provide meaningful error messages:
1. Product existence and active status
2. Quantity validity (1-100 range)
3. Stock availability
4. Customer existence
5. Credit limit compliance
6. Minimum order value (100 SEK)

Early termination on first failure provides fast feedback.

### 6. Repository Pattern
**In-Memory Storage**: Used `ConcurrentHashMap` for thread-safe operations without external dependencies. This simulates database behavior while keeping the solution simple.

### 7. Immutable Results
**PricingResult and ValidationResult** are immutable value objects that encapsulate calculation results, making the code more testable and predictable.

### 8. Error Handling Strategy
- **OrderValidationException**: For business rule violations during order processing
- **InvalidStateTransitionException**: For illegal order status changes
- **ResourceNotFoundException**: For missing entities (orders, customers, products)
- All exceptions extend `RuntimeException` for cleaner service layer code
		
	
### Technical Assumptions
1. **Thread Safety**: `ConcurrentHashMap` provides sufficient concurrency control for in-memory repositories. Inventory operations are synchronized at the method level.
2. **Order IDs**: Generated using `UUID.randomUUID()` to ensure uniqueness.
3. **Timestamps**: Using `Instant` for UTC timestamps. The `updatedOn` field updates automatically on status changes.
4. **BigDecimal Precision**: All monetary calculations use `BigDecimal` with `HALF_UP` rounding to 2 decimal places.
5. **Item Pricing**: Unit prices and line prices are set during order creation based on current product prices (no price history).
6. **Product Availability**: Inactive products fail validation even if in stock.
