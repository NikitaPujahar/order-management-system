package com.oms.service;

import com.oms.exception.InvalidStateTransitionException;
import com.oms.exception.OrderValidationException;
import com.oms.exception.ResourceNotFoundException;
import com.oms.model.*;
import com.oms.repository.CustomerRepository;
import com.oms.repository.OrderRepository;
import com.oms.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class OrderManagementService {
    private static final BigDecimal MIN_ORDER_VALUE = new BigDecimal("100");
    private static final Integer MIN_QUANTITY = 1;
    private static final Integer MAX_QUANTITY = 100;

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PricingEngine pricingEngine;
    private final InventoryManager inventoryManager;

    public OrderManagementService(OrderRepository orderRepository,
                                  CustomerRepository customerRepository,
                                  ProductRepository productRepository,
                                  PricingEngine pricingEngine,
                                  InventoryManager inventoryManager) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.pricingEngine = pricingEngine;
        this.inventoryManager = inventoryManager;
    }

    public Order createOrder(String customerId, List<Item> items, String shippingAddress) {
        String orderId = UUID.randomUUID().toString();

        // Items with product prices
        for (Item item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product " + item.getProductId() + " not found"));
            item.setUnitPrice(product.getPrice());
            item.setLinePrice(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        Order order = new Order(orderId, customerId, items, shippingAddress);
        return orderRepository.save(order);
    }

    public ValidationResult validateOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));

        // Products exist and are active
        for (Item item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product == null || !product.isActive()) {
                return ValidationResult.failure("Product " + item.getProductId() + " is not available");
            }
        }

        // Quantities are valid (1-100)
        for (Item item : order.getItems()) {
            if (item.getQuantity() < MIN_QUANTITY || item.getQuantity() > MAX_QUANTITY) {
                return ValidationResult.failure("Invalid quantity for product " + item.getProductId());
            }
        }

        //Stock is available
        for (Item item : order.getItems()) {
            if (!inventoryManager.checkAvailability(item.getProductId(), item.getQuantity())) {
                return ValidationResult.failure("Insufficient stock for product " + item.getProductId());
            }
        }

        //Customer exists
        Customer customer = customerRepository.findById(order.getCustomerId()).orElse(null);
        if (customer == null) {
            return ValidationResult.failure("Customer not found");
        }

        // Calculate pricing before credit and minimum value checks
        PricingResult pricing = pricingEngine.calculatePricing(order, customer);
        order.setTotalAmount(pricing.getTotalAmount());
        order.setVatAmount(pricing.getVatAmount());
        orderRepository.save(order);

        //Credit limit not exceeded
        if (customer.getAvailableCredit().compareTo(order.getTotalAmount()) < 0) {
            return ValidationResult.failure("Order exceeds customer credit limit");
        }

        //Minimum order value 100 SEK (including VAT)
        if (order.getTotalAmount().compareTo(MIN_ORDER_VALUE) < 0) {
            return ValidationResult.failure("Order total is below minimum value of 100 SEK");
        }

        return ValidationResult.success();
    }

    public Order processOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));

        updateOrderStatus(orderId, OrderStatus.PENDING_VALIDATION);

        ValidationResult validationResult = validateOrder(orderId);

        if (!validationResult.isValid()) {
            updateOrderStatus(orderId, OrderStatus.CANCELLED);
            throw new OrderValidationException(validationResult.getFirstError());
        }

        // Reserve stock
        inventoryManager.reserveStock(orderId, order.getItems());

        // Transition to VALIDATED
        updateOrderStatus(orderId, OrderStatus.VALIDATED);

        return orderRepository.findById(orderId).get();
    }

    public Order updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));

        OrderStatus currentStatus = order.getStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new InvalidStateTransitionException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    public List<Order> findOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        // Any status → CANCELLED (except from FULFILLED)
        if (to == OrderStatus.CANCELLED && from != OrderStatus.FULFILLED) {
            return true;
        }

        // Valid forward transitions
        switch (from) {
            case CREATED:
                return to == OrderStatus.PENDING_VALIDATION;
            case PENDING_VALIDATION:
                return to == OrderStatus.VALIDATED || to == OrderStatus.CANCELLED;
            case VALIDATED:
                return to == OrderStatus.PAID;
            case PAID:
                return to == OrderStatus.FULFILLED;
            default:
                return false;
        }
    }
}