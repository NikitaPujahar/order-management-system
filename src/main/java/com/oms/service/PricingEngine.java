package com.oms.service;

import com.oms.model.Customer;
import com.oms.model.Item;
import com.oms.model.Order;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PricingEngine {
    private static final BigDecimal VAT_RATE = new BigDecimal("0.25");
    private static final BigDecimal VAT_MULTIPLIER = new BigDecimal("1.25");
    private static final BigDecimal BULK_DISCOUNT_RATE = new BigDecimal("0.03");
    private static final BigDecimal BULK_DISCOUNT_THRESHOLD = new BigDecimal("5000");

    public PricingResult calculatePricing(Order order, Customer customer) {
        // Step 1: Calculate base total (sum of all line items with VAT included)
        BigDecimal baseTotal = BigDecimal.ZERO;
        for (Item item : order.getItems()) {
            baseTotal = baseTotal.add(item.getLinePrice());
        }

        // Step 2: Apply customer type discount on the total INCLUDING VAT
        BigDecimal customerDiscount = customer.getType().getDiscountRate();
        BigDecimal afterCustomerDiscount = baseTotal.multiply(BigDecimal.ONE.subtract(customerDiscount));

        // Step 3: Apply bulk discount: Additional 3% if order > 5000 SEK (after customer discount)
        BigDecimal finalTotal = afterCustomerDiscount;
        if (afterCustomerDiscount.compareTo(BULK_DISCOUNT_THRESHOLD) > 0) {
            finalTotal = afterCustomerDiscount.multiply(BigDecimal.ONE.subtract(BULK_DISCOUNT_RATE));
        }

        // Step 4: Calculate VAT amount from the final discounted price
        // Since prices include VAT, VAT = finalTotal - (finalTotal / 1.25)
        // Or: VAT = finalTotal * (0.25 / 1.25) = finalTotal * 0.2
        BigDecimal vatAmount = finalTotal.subtract(finalTotal.divide(VAT_MULTIPLIER, 2, RoundingMode.HALF_UP));

        // Round to 2 decimal places
        finalTotal = finalTotal.setScale(2, RoundingMode.HALF_UP);
        vatAmount = vatAmount.setScale(2, RoundingMode.HALF_UP);

        return new PricingResult(finalTotal, vatAmount);
    }
}