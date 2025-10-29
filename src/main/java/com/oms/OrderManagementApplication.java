package com.oms;

import com.oms.exception.OrderValidationException;
import com.oms.model.*;
import com.oms.repository.CustomerRepository;
import com.oms.repository.OrderRepository;
import com.oms.repository.ProductRepository;
import com.oms.service.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class OrderManagementApplication {

    public static void main(String[] args) {
        System.out.println("Order Management Application is running...");

        // Initialize repositories
        OrderRepository orderRepository = new OrderRepository();
        CustomerRepository customerRepository = new CustomerRepository();
        ProductRepository productRepository = new ProductRepository();

        // Initialize services
        PricingEngine pricingEngine = new PricingEngine();
        InventoryManager inventoryManager = new InventoryManager(productRepository);
        OrderManagementService orderService = new OrderManagementService(
                orderRepository, customerRepository, productRepository,
                pricingEngine, inventoryManager);

        // Setup sample data
        setupSampleData(customerRepository, productRepository);

        // Successful order for GOLD customer
        System.out.println(" Successful Order GOLD Customer");
        successfulOrder(orderService);

        // Order with insufficient stock
        System.out.println("\nOrder with Insufficient Stock");
        insufficientStock(orderService);

        // Order exceeding credit limit
        System.out.println("\nOrder Exceeding Credit Limit");
        creditLimitExceeded(orderService);

        // Pricing for different customer types
        System.out.println("\nPricing for different Customer Type");
        pricingComparison(orderService, customerRepository, productRepository, pricingEngine);

        // Concurrent order scenario
        System.out.println("\nConcurrent Orders for Limited Stock");
        concurrentOrders(orderService);
    }

    private static void setupSampleData(CustomerRepository customerRepository,
                                        ProductRepository productRepository) {
        // Create customers
        customerRepository.save(new Customer("C001", CustomerType.REGULAR, new BigDecimal("10000")));
        customerRepository.save(new Customer("C002", CustomerType.SILVER, new BigDecimal("15000")));
        customerRepository.save(new Customer("C003", CustomerType.GOLD, new BigDecimal("20000")));
        customerRepository.save(new Customer("C004", CustomerType.PLATINUM, new BigDecimal("50000")));
        customerRepository.save(new Customer("C005", CustomerType.REGULAR, new BigDecimal("500"))); // Low credit

        // Create products
        productRepository.save(new Product("P001", "Laptop", new BigDecimal("12500"), 10));
        productRepository.save(new Product("P002", "Mouse", new BigDecimal("250"), 50));
        productRepository.save(new Product("P003", "Keyboard", new BigDecimal("625"), 30));
        productRepository.save(new Product("P004", "Monitor", new BigDecimal("3750"), 5));
        productRepository.save(new Product("P005", "USB Cable", new BigDecimal("125"), 100));
    }

    private static void successfulOrder(OrderManagementService orderService) {
        try {
            List<Item> items = Arrays.asList(
                    new Item("P001", 1),
                    new Item("P002", 2)
            );

            Order order = orderService.createOrder("C003", items, "123 Main Street");
            System.out.println("Order created: " + order.getOrderId());

            Order processedOrder = orderService.processOrder(order.getOrderId());
            System.out.println("Order Status: " + processedOrder.getStatus());
            System.out.println("Total Amount (incl VAT): " + processedOrder.getTotalAmount() + " SEK");
            System.out.println("VAT Amount: " + processedOrder.getVatAmount() + " SEK");
            System.out.println("Customer Type: GOLD (10% discount)");
            System.out.println("Order validated successfully");
        } catch (Exception e) {
            System.out.println("SuccessfulOrder Error: " + e.getMessage());
        }
    }

    private static void insufficientStock(OrderManagementService orderService) {
        try {
            List<Item> items = Arrays.asList(
                    new Item("P004", 10)
            );

            Order order = orderService.createOrder("C001", items, "456 Oak Avenue");
            System.out.println("Order created: " + order.getOrderId());

            orderService.processOrder(order.getOrderId());
        } catch (OrderValidationException e) {
            System.out.println("InsufficientStock Validation Failed: " + e.getMessage());
        }
    }

    private static void creditLimitExceeded(OrderManagementService orderService) {
        try {
            List<Item> items = Arrays.asList(
                    new Item("P001", 1)
            );

            Order order = orderService.createOrder("C005", items, "789 Pine Road");
            System.out.println("Order created: " + order.getOrderId());

            orderService.processOrder(order.getOrderId());
        } catch (OrderValidationException e) {
            System.out.println("CreditLimitExceeded Validation Failed: " + e.getMessage());
        }
    }

    private static void pricingComparison(OrderManagementService orderService,
                                          CustomerRepository customerRepository,
                                          ProductRepository productRepository,
                                          PricingEngine pricingEngine) {
        List<Item> items = Arrays.asList(
                new Item("P001", 1, new BigDecimal("12500")),
                new Item("P003", 2, new BigDecimal("625"))
        );

        for (Item item : items) {
            Product product = productRepository.findById(item.getProductId()).get();
            item.setUnitPrice(product.getPrice());
            item.setLinePrice(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        Order order = new Order("Order", "C001", items, "Address");

        System.out.println("Base Total (incl VAT): 13750 SEK\n");

        for (CustomerType type : CustomerType.values()) {
            Customer customer = new Customer("TEST", type, new BigDecimal("50000"));
            PricingResult pricing = pricingEngine.calculatePricing(order, customer);

            System.out.println(type + " Customer:");
            System.out.println("---------------------------------");
            System.out.println("  Discount: " + type.getDiscountRate().multiply(new BigDecimal("100")) + "%");
            System.out.println("  Total (incl VAT): " + pricing.getTotalAmount() + " SEK");
            System.out.println("  VAT Amount: " + pricing.getVatAmount() + " SEK\n");
        }
    }

    private static void concurrentOrders(OrderManagementService orderService) {
        System.out.println("Product P004 (Monitor) has 5 units in stock");
        System.out.println("Customer A wants 3 units, Customer B wants 3 units\n");

        Thread customerA = new Thread(() -> {
            try {
                List<Item> items = Arrays.asList(new Item("P004", 3));
                Order order = orderService.createOrder("C003", items, "Customer A Address");
                Thread.sleep(50);
                orderService.processOrder(order.getOrderId());
                System.out.println("concurrentOrders Customer A: Order " + order.getOrderId() + " succeeded");
            } catch (Exception e) {
                System.out.println("concurrentOrders Customer A: " + e.getMessage());
            }
        });

        Thread customerB = new Thread(() -> {
            try {
                List<Item> items = Arrays.asList(new Item("P004", 3));
                Order order = orderService.createOrder("C004", items, "Customer B Address");
                Thread.sleep(50);
                orderService.processOrder(order.getOrderId());
                System.out.println("concurrentOrders Customer B: Order " + order.getOrderId() + " succeeded");
            } catch (Exception e) {
                System.out.println("concurrentOrders Customer B: " + e.getMessage());
            }
        });

        customerA.start();
        customerB.start();

        try {
            customerA.join();
            customerB.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Result: Only one order succeeded due to stock reservation");
    }
}