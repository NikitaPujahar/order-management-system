package com.oms.service;

import com.oms.exception.InvalidStateTransitionException;
import com.oms.exception.OrderValidationException;
import com.oms.model.*;
import com.oms.repository.CustomerRepository;
import com.oms.repository.OrderRepository;
import com.oms.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderManagementServiceTest {
    private OrderManagementService orderService;
    private OrderRepository orderRepository;
    private CustomerRepository customerRepository;
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        orderRepository = new OrderRepository();
        customerRepository = new CustomerRepository();
        productRepository = new ProductRepository();

        PricingEngine pricingEngine = new PricingEngine();
        InventoryManager inventoryManager = new InventoryManager(productRepository);

        orderService = new OrderManagementService(
                orderRepository, customerRepository, productRepository,
                pricingEngine, inventoryManager);

        // Setup test data
        customerRepository.save(new Customer("C001", CustomerType.REGULAR, new BigDecimal("10000")));
        customerRepository.save(new Customer("C002", CustomerType.GOLD, new BigDecimal("500")));

        productRepository.save(new Product("P001", "Laptop", new BigDecimal("5000"), 10));
        productRepository.save(new Product("P002", "Mouse", new BigDecimal("200"), 5));

        Product inactiveProduct = new Product("P003", "Discontinued", new BigDecimal("100"), 10);
        inactiveProduct.setActive(false);
        productRepository.save(inactiveProduct);
    }

    @Test
    void testSuccessfulOrderCreation() {
        List<Item> items = Arrays.asList(new Item("P001", 1));
        Order order = orderService.createOrder("C001", items, "123 Main St");

        assertNotNull(order.getOrderId());
        assertEquals("C001", order.getCustomerId());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(1, order.getItems().size());
    }

    @Test
    void testValidationFailsForInactiveProduct() {
        List<Item> items = Arrays.asList(new Item("P003", 1));
        Order order = orderService.createOrder("C001", items, "123 Main St");

        ValidationResult result = orderService.validateOrder(order.getOrderId());

        assertFalse(result.isValid());
        assertTrue(result.getFirstError().contains("not available"));
    }

    @Test
    void testValidationFailsForInvalidQuantity() {
        List<Item> items = Arrays.asList(new Item("P001", 150)); // Over 100
        Order order = orderService.createOrder("C001", items, "123 Main St");

        ValidationResult result = orderService.validateOrder(order.getOrderId());

        assertFalse(result.isValid());
        assertTrue(result.getFirstError().contains("Invalid quantity"));
    }

    @Test
    void testValidationFailsForInsufficientStock() {
        List<Item> items = Arrays.asList(new Item("P002", 10)); // Only 5 available
        Order order = orderService.createOrder("C001", items, "123 Main St");

        ValidationResult result = orderService.validateOrder(order.getOrderId());

        assertFalse(result.isValid());
        assertTrue(result.getFirstError().contains("Insufficient stock"));
    }

    @Test
    void testValidationFailsForCreditLimitExceeded() {
        List<Item> items = Arrays.asList(new Item("P001", 1)); // 5000 SEK, customer has 500 limit
        Order order = orderService.createOrder("C002", items, "123 Main St");

        ValidationResult result = orderService.validateOrder(order.getOrderId());

        assertFalse(result.isValid());
        assertTrue(result.getFirstError().contains("credit limit"));
    }

    @Test
    void testValidationFailsForBelowMinimumValue() {
        productRepository.save(new Product("P004", "Cheap Item", new BigDecimal("50"), 10));
        List<Item> items = Arrays.asList(new Item("P004", 1)); // 50 SEK, min is 100
        Order order = orderService.createOrder("C001", items, "123 Main St");

        ValidationResult result = orderService.validateOrder(order.getOrderId());

        assertFalse(result.isValid());
        assertTrue(result.getFirstError().contains("minimum value"));
    }

    @Test
    void testSuccessfulOrderProcessing() {
        List<Item> items = Arrays.asList(new Item("P001", 1));
        Order order = orderService.createOrder("C001", items, "123 Main St");

        Order processedOrder = orderService.processOrder(order.getOrderId());

        assertEquals(OrderStatus.VALIDATED, processedOrder.getStatus());
        assertNotNull(processedOrder.getTotalAmount());
        assertNotNull(processedOrder.getVatAmount());
    }

    @Test
    void testInvalidStateTransition() {
        List<Item> items = Arrays.asList(new Item("P001", 1));
        Order order = orderService.createOrder("C001", items, "123 Main St");

        assertThrows(InvalidStateTransitionException.class, () -> {
            orderService.updateOrderStatus(order.getOrderId(), OrderStatus.PAID);
        });
    }

    @Test
    void testValidStateTransitions() {
        List<Item> items = Arrays.asList(new Item("P001", 1));
        Order order = orderService.createOrder("C001", items, "123 Main St");

        // Valid transition path
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.PENDING_VALIDATION);
        assertEquals(OrderStatus.PENDING_VALIDATION,
                orderRepository.findById(order.getOrderId()).get().getStatus());

        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.VALIDATED);
        assertEquals(OrderStatus.VALIDATED,
                orderRepository.findById(order.getOrderId()).get().getStatus());

        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.PAID);
        assertEquals(OrderStatus.PAID,
                orderRepository.findById(order.getOrderId()).get().getStatus());

        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.FULFILLED);
        assertEquals(OrderStatus.FULFILLED,
                orderRepository.findById(order.getOrderId()).get().getStatus());
    }

    @Test
    void testCancellationFromAnyStatus() {
        List<Item> items = Arrays.asList(new Item("P001", 1));
        Order order = orderService.createOrder("C001", items, "123 Main St");

        // Can cancel from CREATED
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.CANCELLED);
        assertEquals(OrderStatus.CANCELLED,
                orderRepository.findById(order.getOrderId()).get().getStatus());
    }

    @Test
    void testCannotCancelFromFulfilled() {
        List<Item> items = Arrays.asList(new Item("P001", 1));
        Order order = orderService.createOrder("C001", items, "123 Main St");

        // Move to fulfilled
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.PENDING_VALIDATION);
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.VALIDATED);
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.PAID);
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.FULFILLED);

        // Cannot cancel from FULFILLED
        assertThrows(InvalidStateTransitionException.class, () -> {
            orderService.updateOrderStatus(order.getOrderId(), OrderStatus.CANCELLED);
        });
    }

    @Test
    void testFindOrdersByCustomer() {
        List<Item> items = Arrays.asList(new Item("P001", 1));
        orderService.createOrder("C001", items, "Address 1");
        orderService.createOrder("C001", items, "Address 2");
        orderService.createOrder("C002", items, "Address 3");

        List<Order> c001Orders = orderService.findOrdersByCustomer("C001");
        assertEquals(2, c001Orders.size());

        List<Order> c002Orders = orderService.findOrdersByCustomer("C002");
        assertEquals(1, c002Orders.size());
    }
}