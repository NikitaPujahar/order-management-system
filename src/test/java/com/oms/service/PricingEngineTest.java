package com.oms.service;

import com.oms.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PricingEngineTest {
    private PricingEngine pricingEngine;
    private Order order;

    @BeforeEach
    void setUp() {
        pricingEngine = new PricingEngine();

        List<Item> items = Arrays.asList(
                new Item("P001", 1, new BigDecimal("1000")),
                new Item("P002", 2, new BigDecimal("500"))
        );

        order = new Order("O001", "C001", items, "Test Address");
    }

    @Test
    void testRegularCustomerPricing() {
        Customer customer = new Customer("C001", CustomerType.REGULAR, new BigDecimal("10000"));
        PricingResult result = pricingEngine.calculatePricing(order, customer);

        assertEquals(new BigDecimal("2000.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("400.00"), result.getVatAmount());
    }

    @Test
    void testSilverCustomerPricing() {
        Customer customer = new Customer("C002", CustomerType.SILVER, new BigDecimal("10000"));
        PricingResult result = pricingEngine.calculatePricing(order, customer);

        assertEquals(new BigDecimal("1900.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("380.00"), result.getVatAmount());
    }

    @Test
    void testGoldCustomerPricing() {
        Customer customer = new Customer("C003", CustomerType.GOLD, new BigDecimal("10000"));
        PricingResult result = pricingEngine.calculatePricing(order, customer);

        assertEquals(new BigDecimal("1800.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("360.00"), result.getVatAmount());
    }

    @Test
    void testPlatinumCustomerPricing() {
        Customer customer = new Customer("C004", CustomerType.PLATINUM, new BigDecimal("10000"));
        PricingResult result = pricingEngine.calculatePricing(order, customer);

        assertEquals(new BigDecimal("1700.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("340.00"), result.getVatAmount());
    }

    @Test
    void testBulkDiscountApplied() {
        // Create large order over 5000 SEK
        List<Item> largeItems = Arrays.asList(
                new Item("P001", 10, new BigDecimal("600"))
        );
        Order largeOrder = new Order("O002", "C001", largeItems, "Test Address");
        Customer customer = new Customer("C001", CustomerType.REGULAR, new BigDecimal("20000"));

        PricingResult result = pricingEngine.calculatePricing(largeOrder, customer);

        assertEquals(new BigDecimal("5820.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("1164.00"), result.getVatAmount());
    }

    @Test
    void testBulkDiscountWithCustomerDiscount() {
        // Large order with GOLD customer
        List<Item> largeItems = Arrays.asList(
                new Item("P001", 10, new BigDecimal("600"))
        );
        Order largeOrder = new Order("O003", "C003", largeItems, "Test Address");
        Customer customer = new Customer("C003", CustomerType.GOLD, new BigDecimal("20000"));

        PricingResult result = pricingEngine.calculatePricing(largeOrder, customer);

        assertEquals(new BigDecimal("5238.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("1047.60"), result.getVatAmount());
    }
}