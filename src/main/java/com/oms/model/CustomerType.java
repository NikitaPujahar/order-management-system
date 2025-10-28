package com.oms.model;

import java.math.BigDecimal;

public enum CustomerType {
    REGULAR(BigDecimal.ZERO),
    SILVER(new BigDecimal("0.05")),
    GOLD(new BigDecimal("0.10")),
    PLATINUM(new BigDecimal("0.15"));

    private final BigDecimal discountRate;

    CustomerType(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }
}