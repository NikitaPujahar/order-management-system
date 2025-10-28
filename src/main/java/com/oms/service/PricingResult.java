package com.oms.service;

import java.math.BigDecimal;

public class PricingResult {
    private final BigDecimal totalAmount;
    private final BigDecimal vatAmount;

    public PricingResult(BigDecimal totalAmount, BigDecimal vatAmount) {
        this.totalAmount = totalAmount;
        this.vatAmount = vatAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getVatAmount() {
        return vatAmount;
    }
}