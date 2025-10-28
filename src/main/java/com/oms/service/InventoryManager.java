package com.oms.service;

import com.oms.model.Item;
import com.oms.model.Product;
import com.oms.repository.ProductRepository;
import com.oms.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryManager {
    private final ProductRepository productRepository;
    // Track reserved stock per order: orderId -> Map<productId, quantity>
    private final Map<String, Map<String, Integer>> reservations = new ConcurrentHashMap<>();

    public InventoryManager(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public synchronized boolean checkAvailability(String productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " not found"));

        int reservedQuantity = getReservedQuantity(productId);
        int availableStock = product.getStockQuantity() - reservedQuantity;

        return availableStock >= quantity;
    }

    public synchronized void reserveStock(String orderId, List<Item> items) {
        Map<String, Integer> orderReservations = new ConcurrentHashMap<>();

        // First check all items are available
        for (Item item : items) {
            if (!checkAvailability(item.getProductId(), item.getQuantity())) {
                throw new IllegalStateException("Insufficient stock for product " + item.getProductId());
            }
        }

        // Reserve all items
        for (Item item : items) {
            orderReservations.put(item.getProductId(), item.getQuantity());
        }

        reservations.put(orderId, orderReservations);
    }

    public synchronized void releaseStock(String orderId) {
        reservations.remove(orderId);
    }

    public synchronized void confirmStock(String orderId) {
        Map<String, Integer> orderReservations = reservations.get(orderId);
        if (orderReservations == null) {
            return;
        }

        // Deduct from actual inventory
        for (Map.Entry<String, Integer> entry : orderReservations.entrySet()) {
            String productId = entry.getKey();
            Integer quantity = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " not found"));

            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);
        }

        // Remove reservation
        reservations.remove(orderId);
    }

    private int getReservedQuantity(String productId) {
        int total = 0;
        for (Map<String, Integer> orderReservation : reservations.values()) {
            total += orderReservation.getOrDefault(productId, 0);
        }
        return total;
    }
}