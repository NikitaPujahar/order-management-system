package com.oms.service;

import com.oms.model.Item;
import com.oms.model.Product;
import com.oms.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InventoryManagerTest {
    private InventoryManager inventoryManager;
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository = new ProductRepository();
        inventoryManager = new InventoryManager(productRepository);

        productRepository.save(new Product("P001", "Test Product", new BigDecimal("100"), 10));
    }

    @Test
    void testCheckAvailability() {
        assertTrue(inventoryManager.checkAvailability("P001", 5));
        assertTrue(inventoryManager.checkAvailability("P001", 10));
        assertFalse(inventoryManager.checkAvailability("P001", 11));
    }

    @Test
    void testReserveStock() {
        List<Item> items = Arrays.asList(new Item("P001", 3));
        inventoryManager.reserveStock("O001", items);

        // Stock is reserved, so only 7 available now
        assertTrue(inventoryManager.checkAvailability("P001", 7));
        assertFalse(inventoryManager.checkAvailability("P001", 8));
    }

    @Test
    void testMultipleReservations() {
        List<Item> items1 = Arrays.asList(new Item("P001", 3));
        List<Item> items2 = Arrays.asList(new Item("P001", 4));

        inventoryManager.reserveStock("O001", items1);
        inventoryManager.reserveStock("O002", items2);

        // 3 + 4 = 7 reserved, only 3 available
        assertTrue(inventoryManager.checkAvailability("P001", 3));
        assertFalse(inventoryManager.checkAvailability("P001", 4));
    }

    @Test
    void testReleaseStock() {
        List<Item> items = Arrays.asList(new Item("P001", 5));
        inventoryManager.reserveStock("O001", items);

        assertFalse(inventoryManager.checkAvailability("P001", 6));

        inventoryManager.releaseStock("O001");

        // After release, all 10 available again
        assertTrue(inventoryManager.checkAvailability("P001", 10));
    }

    @Test
    void testConfirmStock() {
        List<Item> items = Arrays.asList(new Item("P001", 3));
        inventoryManager.reserveStock("O001", items);

        Product product = productRepository.findById("P001").get();
        assertEquals(10, product.getStockQuantity()); // Not yet deducted

        inventoryManager.confirmStock("O001");

        product = productRepository.findById("P001").get();
        assertEquals(7, product.getStockQuantity()); // Now deducted

        // All 7 available (no reservation)
        assertTrue(inventoryManager.checkAvailability("P001", 7));
    }

    @Test
    void testReserveInsufficientStock() {
        List<Item> items = Arrays.asList(new Item("P001", 15));

        assertThrows(IllegalStateException.class, () -> {
            inventoryManager.reserveStock("O001", items);
        });
    }

    @Test
    void testConcurrentReservations() throws InterruptedException {
        // Product has 10 units
        // Two threads try to reserve 6 units each
        // Only one should succeed

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        Thread thread1 = new Thread(() -> {
            try {
                List<Item> items = Arrays.asList(new Item("P001", 6));
                inventoryManager.reserveStock("O001", items);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                List<Item> items = Arrays.asList(new Item("P001", 6));
                inventoryManager.reserveStock("O002", items);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        thread1.start();
        thread2.start();
        latch.await();

        // Only one should succeed
        assertEquals(1, successCount.get());
        assertEquals(1, failureCount.get());
    }
}