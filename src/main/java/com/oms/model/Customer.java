package com.oms.model;

import java.math.BigDecimal;

public class Customer {
    private String customerId;
    private CustomerType type;
    private BigDecimal creditLimit;
    private BigDecimal usedCredit;

    public Customer() {
        this.usedCredit = BigDecimal.ZERO;
    }

    public Customer(String customerId, CustomerType type, BigDecimal creditLimit) {
        this();
        this.customerId = customerId;
        this.type = type;
        this.creditLimit = creditLimit;
    }

    public BigDecimal getAvailableCredit() {
        return creditLimit.subtract(usedCredit);
    }

    public String getCustomerId() { return customerId; }
    public CustomerType getType() { return type; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public BigDecimal getUsedCredit() { return usedCredit; }

    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setType(CustomerType type) { this.type = type; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public void setUsedCredit(BigDecimal usedCredit) { this.usedCredit = usedCredit; }
}