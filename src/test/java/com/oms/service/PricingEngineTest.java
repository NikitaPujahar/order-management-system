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

        // Base: 1000 + 1000 = 2000 SEK (incl VAT)
        // No customer discount
        // No bulk discount (under 5000)
        // VAT = 2000 - (2000/1.25) = 400 SEK
        assertEquals(new BigDecimal("2000.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("400.00"), result.getVatAmount());
    }

    @Test
    void testSilverCustomerPricing() {
        Customer customer = new Customer("C002", CustomerType.SILVER, new BigDecimal("10000"));
        PricingResult result = pricingEngine.calculatePricing(order, customer);

        // Base: 2000 SEK
        // Silver discount: 5% = 2000 * 0.95 = 1900 SEK
        // VAT = 1900 - (1900/1.25) = 380 SEK
        assertEquals(new BigDecimal("1900.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("380.00"), result.getVatAmount());
    }

    @Test
    void testGoldCustomerPricing() {
        Customer customer = new Customer("C003", CustomerType.GOLD, new BigDecimal("10000"));
        PricingResult result = pricingEngine.calculatePricing(order, customer);

        // Base: 2000 SEK
        // Gold discount: 10% = 2000 * 0.90 = 1800 SEK
        // VAT = 1800 - (1800/1.25) = 360 SEK
        assertEquals(new BigDecimal("1800.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("360.00"), result.getVatAmount());
    }

    @Test
    void testPlatinumCustomerPricing() {
        Customer customer = new Customer("C004", CustomerType.PLATINUM, new BigDecimal("10000"));
        PricingResult result = pricingEngine.calculatePricing(order, customer);

        // Base: 2000 SEK
        // Platinum discount: 15% = 2000 * 0.85 = 1700 SEK
        // VAT = 1700 - (1700/1.25) = 340 SEK
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

        // Base: 6000 SEK
        // No customer discount (REGULAR)
        // Bulk discount: 3% = 6000 * 0.97 = 5820 SEK
        // VAT = 5820 - (5820/1.25) = 1164 SEK
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

        // Base: 6000 SEK
        // Gold discount: 10% = 6000 * 0.90 = 5400 SEK
        // Bulk discount: 3% = 5400 * 0.97 = 5238 SEK
        // VAT = 5238 - (5238/1.25) = 1047.60 SEK
        assertEquals(new BigDecimal("5238.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("1047.60"), result.getVatAmount());
    }
}